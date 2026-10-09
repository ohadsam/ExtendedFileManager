package com.efm.filemanager.ui.feature.dashboard

import com.efm.filemanager.data.dashboard.DashboardSavedLayout
import com.efm.filemanager.data.dashboard.DashboardWidgetConfig
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
    /**
     * True while the pencil-icon edit mode is active -- a widget's card shows a visibility
     * toggle switch instead of its usual tap-through chevron.
     */
    val isEditMode: Boolean = false,
    /** In catalog ([com.efm.filemanager.domain.model.DashboardWidgetType]) order, not DB row order. */
    val widgetConfigs: List<DashboardWidgetConfig> = emptyList(),
    val savedLayouts: List<DashboardSavedLayout> = emptyList(),
    val freeStorageBytes: Long = 0L,
    /** True once [freeStorageBytes] drops below the user's threshold -- the low-storage card's own automatic show/hide signal. */
    val isLowStorage: Boolean = false,
)
