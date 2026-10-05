package com.efm.filemanager.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.advisor.StorageAdvisorRepository
import com.efm.filemanager.data.duplicates.DuplicateScanRepository
import com.efm.filemanager.data.metadata.FileMetadataRepository
import com.efm.filemanager.data.statistics.StatisticsRepository
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
 * Phase 19 slice 1: a fixed, read-only widget set -- no catalog, no drag-and-drop, no saved
 * layouts, and Browse (not this screen) stays the app's start destination -- all deliberately
 * deferred to later slices (see docs/PLAN.md Phase 19). Every widget reuses an already-live
 * source exactly like Phase 18's own dashboard does: zero new scanning.
 */
@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val statisticsRepository: StatisticsRepository,
        private val duplicateScanRepository: DuplicateScanRepository,
        private val storageAdvisorRepository: StorageAdvisorRepository,
        private val fileMetadataRepository: FileMetadataRepository,
    ) : ViewModel() {
        private val isLoading = MutableStateFlow(true)
        private val storageStats = MutableStateFlow(StorageStats())
        private val favoriteEntries = MutableStateFlow(emptyList<FileEntry>())

        val uiState: StateFlow<DashboardUiState> =
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
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState())

        init {
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
    }
