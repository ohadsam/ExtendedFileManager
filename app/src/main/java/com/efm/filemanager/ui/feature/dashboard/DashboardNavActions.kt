package com.efm.filemanager.ui.feature.dashboard

/** Bundles Dashboard's tap-to-drill-down targets, same shape as `StatisticsNavActions`. */
data class DashboardNavActions(
    val onOpenStatistics: () -> Unit,
    val onOpenDuplicates: () -> Unit,
    val onOpenInsights: () -> Unit,
    val onOpenFavorites: () -> Unit,
    val onOpenSearch: () -> Unit,
)
