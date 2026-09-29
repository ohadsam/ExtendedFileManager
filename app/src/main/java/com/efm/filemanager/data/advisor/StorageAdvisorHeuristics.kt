package com.efm.filemanager.data.advisor

import com.efm.filemanager.domain.model.RecommendationReason

/** Extension-based temp-file patterns -- docs/PLAN.md Phase 10. Matched case-insensitively. */
private val TEMP_FILE_EXTENSIONS = setOf("tmp", "temp", "log", "bak", "cache", "crdownload", "part")

/** Android's own trash-rename prefix for a file pending permanent deletion. */
private const val TRASHED_PREFIX = ".trashed-"

internal const val DEFAULT_LARGE_FILE_MIN_BYTES = 100L * 1024 * 1024
internal const val DEFAULT_UNUSED_THRESHOLD_MILLIS = 1000L * 60 * 60 * 24 * 30 * 6 // ~6 months

/** [largeUnusedReason]'s two tunable thresholds, bundled so the function's own param count stays down. */
internal data class LargeFileThresholds(
    val minSizeBytes: Long = DEFAULT_LARGE_FILE_MIN_BYTES,
    val unusedThresholdMillis: Long = DEFAULT_UNUSED_THRESHOLD_MILLIS,
)

/**
 * Returns the matched pattern (e.g. ".tmp", ".trashed-*") for display, or null if [name] doesn't
 * look temporary. A leftover Android trash-rename is checked by prefix; everything else, by the
 * name's real, final extension -- "notes.tmp.pdf" is a PDF, not a temp file, and correctly
 * doesn't match, while "report.v2.tmp" does.
 */
internal fun matchesTemporaryPattern(name: String): String? {
    if (name.startsWith(TRASHED_PREFIX)) return "$TRASHED_PREFIX*"
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return ".$extension".takeIf { extension in TEMP_FILE_EXTENSIONS }
}

/**
 * True when [folderName] sits directly under an "Android/media" folder (an app-private media
 * folder Android itself leaves behind) and its package no longer resolves -- i.e. the owning app
 * was uninstalled. [ancestorNames] is the folder's own ancestor chain, nearest-parent last.
 * [isPackageInstalled] is a caller-supplied point check rather than a precomputed set, since
 * Android's package-visibility rules (API 30+) make a specific-package lookup the honest way to
 * ask this -- listing every installed app up front would be filtered the same way regardless.
 */
internal fun isOrphanedAppMediaFolder(
    ancestorNames: List<String>,
    folderName: String,
    isPackageInstalled: (String) -> Boolean,
): Boolean = ancestorNames.takeLast(2) == listOf("Android", "media") && !isPackageInstalled(folderName)

/**
 * Scores a candidate file against Phase 10's "large and unused for a long time" recommendation.
 * [lastOpenedAt], when present (the file has been previewed/opened via EFM at least once), takes
 * priority over [lastModified] -- a recent EFM open means the file isn't truly unused even if
 * nothing has touched its content in a while. Returns null when the file doesn't qualify.
 */
internal fun largeUnusedReason(
    sizeBytes: Long,
    lastModified: Long,
    lastOpenedAt: Long?,
    now: Long,
    thresholds: LargeFileThresholds = LargeFileThresholds(),
): RecommendationReason? {
    if (sizeBytes < thresholds.minSizeBytes) return null
    val signalAge = now - (lastOpenedAt ?: lastModified)
    val reason = if (lastOpenedAt != null) RecommendationReason.NOT_OPENED_VIA_APP else RecommendationReason.NOT_MODIFIED_RECENTLY
    return reason.takeIf { signalAge >= thresholds.unusedThresholdMillis }
}
