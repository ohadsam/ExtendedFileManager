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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageAdvisorSettingsState(
    val minSizeMb: Int = 100,
    val unusedMonths: Int = 6,
    val stagedReviewDays: Int = 30,
)

data class InsightsSettingsState(
    val runDailyInsights: Boolean = true,
    val notifyMeEnabled: Boolean = true,
)

data class DashboardSettingsState(
    val lowStorageThresholdMb: Int = 1_000,
)

data class SettingsUiState(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val grantedFolderCount: Int = 0,
    val viewMode: ViewMode = ViewMode.LIST,
    val advisorSettings: StorageAdvisorSettingsState = StorageAdvisorSettingsState(),
    val insightsSettings: InsightsSettingsState = InsightsSettingsState(),
    val dashboardSettings: DashboardSettingsState = DashboardSettingsState(),
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
        private val documentTreeAccessManager: DocumentTreeAccessManager,
    ) : ViewModel() {
        private val grantedFolderCount = MutableStateFlow(currentGrantedFolderCount())

        private val advisorSettings =
            combine(
                preferencesRepository.advisorMinSizeMb,
                preferencesRepository.advisorUnusedMonths,
                preferencesRepository.stagedReviewDays,
            ) { minSizeMb, unusedMonths, stagedReviewDays ->
                StorageAdvisorSettingsState(minSizeMb, unusedMonths, stagedReviewDays)
            }

        private val insightsSettings =
            combine(preferencesRepository.runDailyInsights, preferencesRepository.notifyMeEnabled) { runEnabled, notifyEnabled ->
                InsightsSettingsState(runEnabled, notifyEnabled)
            }

        private val dashboardSettings =
            preferencesRepository.lowStorageThresholdMb.map { thresholdMb -> DashboardSettingsState(thresholdMb) }

        private val baseUiState =
            combine(
                preferencesRepository.appearanceMode,
                preferencesRepository.dynamicColorEnabled,
                grantedFolderCount,
                preferencesRepository.viewMode,
                advisorSettings,
            ) { appearanceMode, dynamicColorEnabled, folderCount, viewMode, advisor ->
                SettingsUiState(appearanceMode, dynamicColorEnabled, folderCount, viewMode, advisor)
            }

        val uiState: StateFlow<SettingsUiState> =
            combine(baseUiState, insightsSettings, dashboardSettings) { base, insights, dashboard ->
                base.copy(insightsSettings = insights, dashboardSettings = dashboard)
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

        fun setAdvisorMinSizeMb(mb: Int) {
            viewModelScope.launch { preferencesRepository.setAdvisorMinSizeMb(mb) }
        }

        fun setAdvisorUnusedMonths(months: Int) {
            viewModelScope.launch { preferencesRepository.setAdvisorUnusedMonths(months) }
        }

        fun setStagedReviewDays(days: Int) {
            viewModelScope.launch { preferencesRepository.setStagedReviewDays(days) }
        }

        fun setRunDailyInsights(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.setRunDailyInsights(enabled) }
        }

        fun setNotifyMeEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.setNotifyMeEnabled(enabled) }
        }

        fun setLowStorageThresholdMb(mb: Int) {
            viewModelScope.launch { preferencesRepository.setLowStorageThresholdMb(mb) }
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
