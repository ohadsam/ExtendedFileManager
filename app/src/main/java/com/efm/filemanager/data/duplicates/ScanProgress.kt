package com.efm.filemanager.data.duplicates

import androidx.work.Data
import androidx.work.workDataOf

enum class ScanPhase { COMPARING, VERIFYING }

data class ScanProgress(
    val phase: ScanPhase,
    val current: Int,
    val total: Int,
)

internal const val SCAN_WORK_NAME = "duplicate_scan"
internal const val KEY_RESULT_COUNT = "resultCount"
private const val KEY_PHASE = "phase"
private const val KEY_CURRENT = "current"
private const val KEY_TOTAL = "total"

internal fun ScanProgress.toWorkData(): Data =
    workDataOf(
        KEY_PHASE to phase.name,
        KEY_CURRENT to current,
        KEY_TOTAL to total,
    )

internal fun Data.toScanProgress(): ScanProgress? {
    val phase = getString(KEY_PHASE)?.let { name -> runCatching { ScanPhase.valueOf(name) }.getOrNull() } ?: return null
    return ScanProgress(phase = phase, current = getInt(KEY_CURRENT, 0), total = getInt(KEY_TOTAL, 0))
}
