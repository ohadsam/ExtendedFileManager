package com.efm.filemanager.ui.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.domain.model.AppearanceMode
import com.efm.filemanager.domain.model.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val grantedFolderCount: Int = 0,
    val viewMode: ViewMode = ViewMode.LIST,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
        private val documentTreeAccessManager: DocumentTreeAccessManager,
    ) : ViewModel() {
        private val grantedFolderCount = MutableStateFlow(currentGrantedFolderCount())

        val uiState: StateFlow<SettingsUiState> =
            combine(
                preferencesRepository.appearanceMode,
                preferencesRepository.dynamicColorEnabled,
                grantedFolderCount,
                preferencesRepository.viewMode,
            ) { appearanceMode, dynamicColorEnabled, folderCount, viewMode ->
                SettingsUiState(appearanceMode, dynamicColorEnabled, folderCount, viewMode)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

        fun setAppearanceMode(mode: AppearanceMode) {
            viewModelScope.launch { preferencesRepository.setAppearanceMode(mode) }
        }

        fun setDynamicColorEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.setDynamicColorEnabled(enabled) }
        }

        fun setViewMode(mode: ViewMode) {
            viewModelScope.launch { preferencesRepository.setViewMode(mode) }
        }

        fun refreshPermissionsStatus() {
            grantedFolderCount.value = currentGrantedFolderCount()
        }

        fun currentLanguageOption(): LanguageOption =
            languageOptionFromLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())

        fun setLanguage(option: LanguageOption) {
            val tag = option.toLanguageTag()
            val locales = if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
            AppCompatDelegate.setApplicationLocales(locales)
        }

        private fun currentGrantedFolderCount(): Int = documentTreeAccessManager.grantedTreeUris().size

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
