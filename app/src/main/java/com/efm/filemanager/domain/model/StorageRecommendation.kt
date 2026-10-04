package com.efm.filemanager.domain.model

/**
 * Phase 10's three recommendation categories, Phase 17's lightweight "unclear extension" hygiene
 * nudge, and [DUPLICATE] -- Phase 17 folding Phase 6's duplicate-scan results into this same
 * screen instead of keeping them a separate system (see [toDuplicateRecommendations]).
 */
enum class StorageRecommendationCategory { LARGE_UNUSED, JUNK, TEMPORARY, UNCLEAR_EXTENSION, DUPLICATE }

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
    DUPLICATE_CONTENT,
}

/** One flagged file/folder, always explaining itself via [reason] rather than a bare "junk" label. */
data class StorageRecommendation(
    val entry: FileEntry,
    val category: StorageRecommendationCategory,
    val reason: RecommendationReason,
    /** Extra context for [reason], e.g. the matched temp-file pattern or the orphaned package name. */
    val detail: String? = null,
)

/**
 * Flattens every file in every [DuplicateGroup] into its own [StorageRecommendation] row so
 * Phase 10's existing flat-list-plus-category-header UI can show duplicates alongside its other
 * categories with zero screen-layer changes. [StorageRecommendation.detail] carries how many
 * other files share this one's content, e.g. "2" for a 3-file group, so the UI can say "+2
 * copies" without the row needing to know its own group membership.
 */
fun List<DuplicateGroup>.toDuplicateRecommendations(): List<StorageRecommendation> =
    flatMap { group ->
        group.files.map { entry ->
            StorageRecommendation(
                entry = entry,
                category = StorageRecommendationCategory.DUPLICATE,
                reason = RecommendationReason.DUPLICATE_CONTENT,
                detail = (group.files.size - 1).toString(),
            )
        }
    }
