package com.efm.filemanager.ui.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.vault.VaultAuthRepository
import com.efm.filemanager.data.vault.VaultRepository
import com.efm.filemanager.data.vault.VaultUnlockState
import com.efm.filemanager.domain.model.VaultEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

sealed interface VaultEvent {
    data object WrongPassword : VaultEvent

    data object PasswordsDoNotMatch : VaultEvent

    data object OperationFailed : VaultEvent
}

@HiltViewModel
class VaultViewModel
    @Inject
    constructor(
        private val vaultAuthRepository: VaultAuthRepository,
        private val vaultRepository: VaultRepository,
        private val vaultUnlockState: VaultUnlockState,
    ) : ViewModel() {
        val isUnlocked: StateFlow<Boolean> = vaultUnlockState.isUnlocked

        val entries: StateFlow<List<VaultEntry>> =
            vaultRepository.observeEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val isPasswordSet: Boolean get() = vaultAuthRepository.isPasswordSet()

        private val _events = MutableSharedFlow<VaultEvent>()
        val events: SharedFlow<VaultEvent> = _events.asSharedFlow()

        fun setPassword(
            password: CharArray,
            confirmPassword: CharArray,
        ) {
            if (!password.contentEquals(confirmPassword)) {
                viewModelScope.launch { _events.emit(VaultEvent.PasswordsDoNotMatch) }
                return
            }
            viewModelScope.launch {
                vaultAuthRepository.setPassword(password)
                vaultUnlockState.markUnlocked()
            }
        }

        fun unlockWithPassword(password: CharArray) {
            viewModelScope.launch {
                if (vaultAuthRepository.verifyPassword(password)) {
                    vaultUnlockState.markUnlocked()
                } else {
                    _events.emit(VaultEvent.WrongPassword)
                }
            }
        }

        fun lock() = vaultUnlockState.lock()

        fun removeEntries(ids: List<Long>) {
            viewModelScope.launch {
                val results = ids.map { id -> vaultRepository.removeFromVault(id) }
                if (results.any { it.isFailure }) _events.emit(VaultEvent.OperationFailed)
            }
        }
    }
