package com.efm.filemanager.ui.feature.statistics

import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.GlobalFilesSort

/** Bundles Statistics' tap-to-drill-down targets so the screen's own composable stays under a handful of params. */
data class StatisticsNavActions(
    val onOpenDuplicates: () -> Unit,
    val onOpenInsights: () -> Unit,
    val onOpenAudit: () -> Unit,
    /** Category null means "every file, no category filter" -- the largest-files/recently-modified widgets' own drill-downs. */
    val onOpenGlobalFiles: (FileCategory?, GlobalFilesSort) -> Unit,
    /** The most-populated-folders widget's own drill-down, given the folder's cached URI string. */
    val onOpenFolder: (String) -> Unit,
)
