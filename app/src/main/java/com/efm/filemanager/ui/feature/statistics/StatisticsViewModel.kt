package com.efm.filemanager.ui.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.advisor.StorageAdvisorRepository
import com.efm.filemanager.data.audit.AuditRepository
import com.efm.filemanager.data.duplicates.DuplicateScanRepository
import com.efm.filemanager.data.statistics.StatisticsRepository
import com.efm.filemanager.domain.model.StorageStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Phase 18's first slice: a few widgets backed by data that already exists elsewhere in the
 * app (Phase 6's duplicate cache, Phase 10/17's recommendation cache, Phase 13's audit trail),
 * plus one new lightweight computation ([StatisticsRepository.computeStorageStats]) for the one
 * thing nothing else already tracks -- total size/largest-files/type-breakdown across every
 * granted tree. Charts, drill-down into the owning screen, and a `stats_cache`/`changeVersion`
 * incremental-recompute table are deliberately left for a later slice (see docs/PLAN.md Phase 18).
 */
@HiltViewModel
class StatisticsViewModel
    @Inject
    constructor(
        private val statisticsRepository: StatisticsRepository,
        private val duplicateScanRepository: DuplicateScanRepository,
        private val storageAdvisorRepository: StorageAdvisorRepository,
        private val auditRepository: AuditRepository,
    ) : ViewModel() {
        private val storageStats = MutableStateFlow(StorageStats())
        private val isLoading = MutableStateFlow(true)

        val uiState: StateFlow<StatisticsUiState> =
            combine(
                isLoading,
                storageStats,
                duplicateScanRepository.observeGroups(),
                storageAdvisorRepository.observeRecommendations(),
                auditRepository.observeAll(),
            ) { loading, stats, duplicateGroups, recommendations, auditEvents ->
                StatisticsUiState(
                    isLoading = loading,
                    storageStats = stats,
                    duplicateGroupCount = duplicateGroups.size,
                    reclaimableBytes = duplicateGroups.sumOf { it.fileSize * (it.files.size - 1) },
                    advisorFlaggedCount = recommendations.distinctBy { it.entry.uri }.size,
                    totalOperationsCount = auditEvents.size,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatisticsUiState())

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                isLoading.value = true
                storageStats.value = statisticsRepository.computeStorageStats()
                isLoading.value = false
            }
        }
    }
