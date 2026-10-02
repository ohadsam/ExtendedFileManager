package com.efm.filemanager.ui.feature.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.documenttree.LockedFileException
import com.efm.filemanager.data.metadata.ScreenMetadataSupport
import com.efm.filemanager.data.metadata.SelectionMetadataActions
import com.efm.filemanager.data.vault.VaultRepository
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag
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

sealed interface PreviewEvent {
    data class AddToVaultBlockedByLock(val lockedEntries: List<FileEntry>) : PreviewEvent

    data object OperationFailed : PreviewEvent
}

@HiltViewModel
class PreviewViewModel
    @Inject
    constructor(
        private val previewSessionHolder: PreviewSessionHolder,
        private val vaultRepository: VaultRepository,
        private val screenMetadataSupport: ScreenMetadataSupport,
    ) : ViewModel() {
        val session: StateFlow<PreviewSession?> = previewSessionHolder.session

        val metadataActions: SelectionMetadataActions get() = screenMetadataSupport.metadataActions

        private val _events = MutableSharedFlow<PreviewEvent>()
        val events: SharedFlow<PreviewEvent> = _events.asSharedFlow()

        val tags: StateFlow<List<FileTag>> =
            screenMetadataSupport.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val favoriteCollections: StateFlow<List<FavoriteCollection>> =
            screenMetadataSupport.favoriteCollections.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                emptyList(),
            )

        fun addToVault(entries: List<FileEntry>) {
            viewModelScope.launch {
                val results = entries.associateWith { entry -> vaultRepository.addToVault(entry) }
                val lockedEntries = results.filterValues { it.exceptionOrNull() is LockedFileException }.keys.toList()
                val otherFailureCount = entries.size - results.count { it.value.isSuccess } - lockedEntries.size
                if (lockedEntries.isNotEmpty()) _events.emit(PreviewEvent.AddToVaultBlockedByLock(lockedEntries))
                if (otherFailureCount > 0) _events.emit(PreviewEvent.OperationFailed)
            }
        }

        override fun onCleared() {
            previewSessionHolder.clear()
        }
    }
