package com.efm.filemanager.ui.feature.statistics

import com.efm.filemanager.domain.model.FileCategory

/** Bundles Statistics' tap-to-drill-down targets so the screen's own composable stays under a handful of params. */
data class StatisticsNavActions(
    val onOpenDuplicates: () -> Unit,
    val onOpenInsights: () -> Unit,
    val onOpenAudit: () -> Unit,
    /** null means "every file, no category filter" -- the largest-files widget's own drill-down. */
    val onOpenGlobalFiles: (FileCategory?) -> Unit,
)
