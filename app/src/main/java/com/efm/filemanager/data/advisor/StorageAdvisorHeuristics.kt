package com.efm.filemanager.data.advisor

import com.efm.filemanager.domain.model.RecommendationReason

/** Extension-based temp-file patterns -- docs/PLAN.md Phase 10. Matched case-insensitively. */
private val TEMP_FILE_EXTENSIONS = setOf("tmp", "temp", "log", "bak", "cache", "crdownload", "part")

/** Android's own trash-rename prefix for a file pending permanent deletion. */
private const val TRASHED_PREFIX = ".trashed-"

/**
 * Deliberately broad so ordinary files are never flagged -- this is a hygiene nudge ("worth a
 * look"), never a security verdict, so a false positive here is a real annoyance, not a minor one.
 */
private val KNOWN_EXTENSIONS =
    setOf(
        // images
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg", "ico", "tiff", "tif", "raw",
        // video
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "mpeg", "mpg",
        // audio
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "mid", "midi",
        // documents
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "odt", "ods", "odp", "rtf", "csv", "md", "epub", "mobi",
        // archives and packages
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "apk",
        // code, markup, and config
        "json", "xml", "yaml", "yml", "html", "htm", "css", "js", "ts", "kt", "java", "py", "c", "cpp", "h", "sh", "ini", "cfg", "conf",
        // fonts and logs
        "ttf", "otf", "woff", "woff2", "log",
    )

/** A final extension this common after a preceding one is specifically worth calling out, not just "unrecognized." */
private val SUSPICIOUS_FINAL_EXTENSIONS = setOf("exe", "bat", "cmd", "scr", "msi", "com", "vbs", "ps1", "apk")

internal const val MILLIS_PER_MONTH = 1000L * 60 * 60 * 24 * 30
internal const val BYTES_PER_MB = 1024L * 1024

internal const val DEFAULT_ADVISOR_MIN_SIZE_MB = 100
internal const val DEFAULT_ADVISOR_UNUSED_MONTHS = 6

/** [largeUnusedReason]'s two tunable thresholds, bundled so the function's own param count stays down. */
internal data class LargeFileThresholds(
    val minSizeBytes: Long = DEFAULT_ADVISOR_MIN_SIZE_MB * BYTES_PER_MB,
    val unusedThresholdMillis: Long = DEFAULT_ADVISOR_UNUSED_MONTHS * MILLIS_PER_MONTH,
)

/** Builds [LargeFileThresholds] from the Settings-configurable, user-facing units (megabytes, months). */
internal fun thresholdsFrom(
    minSizeMb: Int,
    unusedMonths: Int,
): LargeFileThresholds =
    LargeFileThresholds(minSizeBytes = minSizeMb * BYTES_PER_MB, unusedThresholdMillis = unusedMonths * MILLIS_PER_MONTH)

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
 * Phase 17's "worth a look" hygiene nudge, never a security verdict: a name with no extension at
 * all (common and normal for many files) isn't flagged, only one whose real, final extension is
 * neither a recognized type nor explained by [matchesTemporaryPattern] -- callers should check
 * that first, since a `.tmp`/`.bak`/etc. file is already explained, not "unclear." A final
 * extension from [SUSPICIOUS_FINAL_EXTENSIONS] preceded by at least one other segment (e.g.
 * "invoice.pdf.exe") is called out specifically, rather than lumped in with plain "unrecognized."
 */
internal fun unclearExtensionReason(name: String): RecommendationReason? {
    val segments = name.split('.')
    if (segments.size < 2) return null
    val finalExtension = segments.last().lowercase()
    val isSuspiciousDouble = segments.size >= 3 && finalExtension in SUSPICIOUS_FINAL_EXTENSIONS
    return when {
        isSuspiciousDouble -> RecommendationReason.SUSPICIOUS_DOUBLE_EXTENSION
        finalExtension !in KNOWN_EXTENSIONS -> RecommendationReason.UNRECOGNIZED_EXTENSION
        else -> null
    }
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
