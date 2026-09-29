package com.efm.filemanager.data.local

import androidx.room.Entity

/**
 * One flagged file/folder from Phase 10's storage advisor scan. Primary key is [uri]+[category]
 * rather than [uri] alone, since the same file can honestly qualify for more than one category
 * at once (e.g. a large, long-untouched .log file is both TEMPORARY and LARGE_UNUSED).
 */
@Entity(tableName = "storage_recommendations", primaryKeys = ["uri", "category"])
data class StorageRecommendationEntity(
    val uri: String,
    val parentUri: String,
    val documentId: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val category: String,
    val reason: String,
    val detail: String?,
)
