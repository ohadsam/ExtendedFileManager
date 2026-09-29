package com.efm.filemanager.data.local

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.RecommendationReason
import com.efm.filemanager.domain.model.StorageRecommendation
import com.efm.filemanager.domain.model.StorageRecommendationCategory

fun FileEntryEntity.toRecommendationEntity(
    category: StorageRecommendationCategory,
    reason: RecommendationReason,
    detail: String? = null,
): StorageRecommendationEntity =
    StorageRecommendationEntity(
        uri = uri,
        parentUri = parentUri,
        documentId = documentId,
        name = name,
        isDirectory = isDirectory,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        category = category.name,
        reason = reason.name,
        detail = detail,
    )

/** Returns null for a row whose [StorageRecommendationEntity.category]/[StorageRecommendationEntity.reason] no longer parses. */
fun StorageRecommendationEntity.toDomain(): StorageRecommendation? {
    val parsedCategory = runCatching { StorageRecommendationCategory.valueOf(category) }.getOrNull() ?: return null
    val parsedReason = runCatching { RecommendationReason.valueOf(reason) }.getOrNull() ?: return null
    return StorageRecommendation(
        entry =
            FileEntry(
                uri = Uri.parse(uri),
                documentId = documentId,
                name = name,
                isDirectory = isDirectory,
                size = size,
                lastModified = lastModified,
                mimeType = mimeType,
                sourceApp = null,
            ),
        category = parsedCategory,
        reason = parsedReason,
        detail = detail,
    )
}
