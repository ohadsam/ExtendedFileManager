package com.efm.filemanager.ui.feature.dashboard

import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.ui.feature.statistics.DuplicatesSummary

data class DashboardUiState(
    val isLoading: Boolean = true,
    val storageStats: StorageStats = StorageStats(),
    val duplicatesSummary: DuplicatesSummary = DuplicatesSummary(),
    val advisorFlaggedCount: Int = 0,
    /** Top few favorites only -- this is a glance-and-go shortcut, not Phase 9's full Favorites screen. */
    val favoriteEntries: List<FileEntry> = emptyList(),
)
