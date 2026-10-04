package com.efm.filemanager.ui.feature.statistics

import com.efm.filemanager.domain.model.StorageStats

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val storageStats: StorageStats = StorageStats(),
    val duplicateGroupCount: Int = 0,
    val reclaimableBytes: Long = 0L,
    val advisorFlaggedCount: Int = 0,
    val totalOperationsCount: Int = 0,
)
