package com.efm.filemanager.ui.feature.advisor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.RecommendationReason
import com.efm.filemanager.domain.model.StorageRecommendation
import com.efm.filemanager.domain.model.StorageRecommendationCategory

internal fun StorageRecommendationCategory.labelRes(): Int =
    when (this) {
        StorageRecommendationCategory.LARGE_UNUSED -> R.string.storage_advisor_category_large_unused
        StorageRecommendationCategory.JUNK -> R.string.storage_advisor_category_junk
        StorageRecommendationCategory.TEMPORARY -> R.string.storage_advisor_category_temporary
        StorageRecommendationCategory.UNCLEAR_EXTENSION -> R.string.storage_advisor_category_unclear_extension
        StorageRecommendationCategory.DUPLICATE -> R.string.storage_advisor_category_duplicate
    }

internal fun RecommendationReason.labelRes(): Int =
    when (this) {
        RecommendationReason.NOT_OPENED_VIA_APP -> R.string.storage_advisor_reason_not_opened_via_app
        RecommendationReason.NOT_MODIFIED_RECENTLY -> R.string.storage_advisor_reason_not_modified_recently
        RecommendationReason.ORPHANED_APP_FOLDER -> R.string.storage_advisor_reason_orphaned_app_folder
        RecommendationReason.EMPTY_FOLDER -> R.string.storage_advisor_reason_empty_folder
        RecommendationReason.APP_CACHE -> R.string.storage_advisor_reason_app_cache
        RecommendationReason.TEMP_FILE_PATTERN -> R.string.storage_advisor_reason_temp_file_pattern
        RecommendationReason.UNRECOGNIZED_EXTENSION -> R.string.storage_advisor_reason_unrecognized_extension
        RecommendationReason.SUSPICIOUS_DOUBLE_EXTENSION -> R.string.storage_advisor_reason_suspicious_double_extension
        RecommendationReason.DUPLICATE_CONTENT -> R.string.storage_advisor_reason_duplicate_content
    }

/**
 * [RecommendationReason.TEMP_FILE_PATTERN]/[RecommendationReason.UNRECOGNIZED_EXTENSION]/
 * [RecommendationReason.SUSPICIOUS_DOUBLE_EXTENSION]/[RecommendationReason.DUPLICATE_CONTENT]
 * carry their matched pattern/extension/other-copy-count as [StorageRecommendation.detail]
 * (hence the plain null check -- every other reason's [detail] is always null); the rest need
 * no args.
 */
@Composable
internal fun StorageRecommendation.reasonText(): String =
    if (detail != null) stringResource(reason.labelRes(), detail) else stringResource(reason.labelRes())
