package com.efm.filemanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.domain.model.AppearanceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppThemeUiState(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
)

@HiltViewModel
class AppThemeViewModel
    @Inject
    constructor(
        preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        val uiState: StateFlow<AppThemeUiState> =
            combine(
                preferencesRepository.appearanceMode,
                preferencesRepository.dynamicColorEnabled,
            ) { appearanceMode, dynamicColorEnabled ->
                AppThemeUiState(appearanceMode, dynamicColorEnabled)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppThemeUiState())

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
