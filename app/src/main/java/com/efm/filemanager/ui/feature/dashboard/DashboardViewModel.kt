package com.efm.filemanager.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.advisor.StorageAdvisorRepository
import com.efm.filemanager.data.dashboard.DashboardLayoutRepository
import com.efm.filemanager.data.dashboard.DashboardSavedLayout
import com.efm.filemanager.data.duplicates.DuplicateScanRepository
import com.efm.filemanager.data.metadata.FileMetadataRepository
import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.data.statistics.StatisticsRepository
import com.efm.filemanager.domain.model.DashboardTemplate
import com.efm.filemanager.domain.model.DashboardWidgetSize
import com.efm.filemanager.domain.model.DashboardWidgetType
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.ui.feature.statistics.DuplicatesSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val MAX_FAVORITES_SHOWN = 5

/**
 * Phase 19 slice 1 shipped a fixed, read-only widget set; slices 2-5 added show/hide, reordering
 * (the exact same drag-handle-drives-a-swap technique
 * [com.efm.filemanager.ui.feature.favorites.FavoritesViewModel.moveCollection] already uses for
 * Favorites collections), size (COMPACT vs. DETAILED), and built-in [DashboardTemplate]s. This
 * slice adds the user's own named saved layouts -- `saveCurrentAsLayout` snapshots today's
 * widget rows, `applySavedLayout` rewrites them back. The start-destination promotion is still
 * open (see docs/PLAN.md Phase 19).
 */
@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val statisticsRepository: StatisticsRepository,
        private val duplicateScanRepository: DuplicateScanRepository,
        private val storageAdvisorRepository: StorageAdvisorRepository,
        private val fileMetadataRepository: FileMetadataRepository,
        private val dashboardLayoutRepository: DashboardLayoutRepository,
    ) : ViewModel() {
        private val isLoading = MutableStateFlow(true)
        private val storageStats = MutableStateFlow(StorageStats())
        private val favoriteEntries = MutableStateFlow(emptyList<FileEntry>())
        private val isEditMode = MutableStateFlow(false)

        private val baseUiState =
            combine(
                isLoading,
                storageStats,
                favoriteEntries,
                duplicateScanRepository.observeGroups(),
                storageAdvisorRepository.observeRecommendations(),
            ) { loading, stats, favorites, duplicateGroups, recommendations ->
                DashboardUiState(
                    isLoading = loading,
                    storageStats = stats,
                    duplicatesSummary =
                        DuplicatesSummary(
                            groupCount = duplicateGroups.size,
                            reclaimableBytes = duplicateGroups.sumOf { it.fileSize * (it.files.size - 1) },
                        ),
                    advisorFlaggedCount = recommendations.distinctBy { it.entry.uri }.size,
                    favoriteEntries = favorites,
                )
            }

        val uiState: StateFlow<DashboardUiState> =
            combine(
                baseUiState,
                isEditMode,
                dashboardLayoutRepository.observeWidgets(),
                dashboardLayoutRepository.observeSavedLayouts(),
            ) { base, editMode, configs, savedLayouts ->
                base.copy(isEditMode = editMode, widgetConfigs = configs, savedLayouts = savedLayouts)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState())

        init {
            viewModelScope.launch { dashboardLayoutRepository.ensureSeeded() }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                isLoading.value = true
                storageStats.value = statisticsRepository.computeStorageStats()
                favoriteEntries.value = fileMetadataRepository.resolveFavoriteEntries().take(MAX_FAVORITES_SHOWN)
                isLoading.value = false
            }
        }

        fun toggleEditMode() {
            isEditMode.value = !isEditMode.value
        }

        fun setWidgetEnabled(
            type: DashboardWidgetType,
            isEnabled: Boolean,
        ) {
            viewModelScope.launch { dashboardLayoutRepository.setEnabled(type, isEnabled) }
        }

        fun moveWidget(
            type: DashboardWidgetType,
            direction: MoveDirection,
        ) {
            viewModelScope.launch { dashboardLayoutRepository.moveWidget(type, direction) }
        }

        fun setWidgetSize(
            type: DashboardWidgetType,
            size: DashboardWidgetSize,
        ) {
            viewModelScope.launch { dashboardLayoutRepository.setSize(type, size) }
        }

        fun applyTemplate(template: DashboardTemplate) {
            viewModelScope.launch { dashboardLayoutRepository.applyTemplate(template) }
        }

        fun saveCurrentAsLayout(name: String) {
            viewModelScope.launch { dashboardLayoutRepository.saveCurrentAsLayout(name) }
        }

        fun applySavedLayout(layout: DashboardSavedLayout) {
            viewModelScope.launch { dashboardLayoutRepository.applySavedLayout(layout.id) }
        }

        fun renameSavedLayout(
            layout: DashboardSavedLayout,
            name: String,
        ) {
            viewModelScope.launch { dashboardLayoutRepository.renameSavedLayout(layout.id, name) }
        }

        fun deleteSavedLayout(layout: DashboardSavedLayout) {
            viewModelScope.launch { dashboardLayoutRepository.deleteSavedLayout(layout.id) }
        }
    }
