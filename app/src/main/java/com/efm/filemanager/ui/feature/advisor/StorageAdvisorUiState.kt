package com.efm.filemanager.ui.feature.advisor

import android.net.Uri
import com.efm.filemanager.data.advisor.AdvisorScanProgress
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageRecommendation
import com.efm.filemanager.domain.model.StorageRecommendationCategory

enum class AdvisorScanRunState { IDLE, RUNNING, SUCCEEDED, FAILED, CANCELLED }

enum class AdvisorTab { RECOMMENDATIONS, STAGED }

data class StorageAdvisorUiState(
    val runState: AdvisorScanRunState = AdvisorScanRunState.IDLE,
    val progress: AdvisorScanProgress? = null,
    val recommendations: List<StorageRecommendation> = emptyList(),
    val stagedEntries: List<FileEntry> = emptyList(),
)

/** Identifies one recommendation row -- a file's uri alone isn't unique, since it can qualify for more than one category. */
data class RecommendationKey(
    val uri: Uri,
    val category: StorageRecommendationCategory,
)

fun StorageRecommendation.key(): RecommendationKey = RecommendationKey(entry.uri, category)
