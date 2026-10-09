package com.efm.filemanager.ui.feature.statistics

import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.StorageStats

data class DuplicatesSummary(
    val groupCount: Int = 0,
    val reclaimableBytes: Long = 0L,
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val storageStats: StorageStats = StorageStats(),
    /** One point per day Phase 18 has run -- fewer than two means "not enough data yet," never charted. */
    val storageTrend: List<Long> = emptyList(),
    /** Same shape as [storageTrend], per [FileCategory] -- each entry charted on its own, never all seven at once. */
    val categoryTrends: Map<FileCategory, List<Long>> = emptyMap(),
    val duplicatesSummary: DuplicatesSummary = DuplicatesSummary(),
    val advisorFlaggedCount: Int = 0,
    val totalOperationsCount: Int = 0,
)
