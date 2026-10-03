package com.efm.filemanager.domain.model

/** Phase 10's three recommendation categories, plus Phase 17's lightweight "unclear extension" hygiene nudge. */
enum class StorageRecommendationCategory { LARGE_UNUSED, JUNK, TEMPORARY, UNCLEAR_EXTENSION }

/**
 * Why a file was recommended, kept distinct from the category since a category can have more
 * than one honest reason (large-unused is scored from whichever signal is actually available).
 */
enum class RecommendationReason {
    NOT_OPENED_VIA_APP,
    NOT_MODIFIED_RECENTLY,
    ORPHANED_APP_FOLDER,
    EMPTY_FOLDER,
    APP_CACHE,
    TEMP_FILE_PATTERN,
    UNRECOGNIZED_EXTENSION,
    SUSPICIOUS_DOUBLE_EXTENSION,
}

/** One flagged file/folder, always explaining itself via [reason] rather than a bare "junk" label. */
data class StorageRecommendation(
    val entry: FileEntry,
    val category: StorageRecommendationCategory,
    val reason: RecommendationReason,
    /** Extra context for [reason], e.g. the matched temp-file pattern or the orphaned package name. */
    val detail: String? = null,
)
