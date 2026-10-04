package com.efm.filemanager.ui.feature.statistics

/** Bundles Statistics' tap-to-drill-down targets so the screen's own composable stays under a handful of params. */
data class StatisticsNavActions(
    val onOpenDuplicates: () -> Unit,
    val onOpenInsights: () -> Unit,
    val onOpenAudit: () -> Unit,
)
