package com.efm.filemanager.data.advisor

import androidx.work.Data
import androidx.work.workDataOf

data class AdvisorScanProgress(val filesScanned: Int)

internal const val ADVISOR_SCAN_WORK_NAME = "storage_advisor_scan"
internal const val KEY_ADVISOR_RESULT_COUNT = "resultCount"
private const val KEY_FILES_SCANNED = "filesScanned"

internal fun AdvisorScanProgress.toWorkData(): Data = workDataOf(KEY_FILES_SCANNED to filesScanned)

internal fun Data.toAdvisorScanProgress(): AdvisorScanProgress = AdvisorScanProgress(filesScanned = getInt(KEY_FILES_SCANNED, 0))
