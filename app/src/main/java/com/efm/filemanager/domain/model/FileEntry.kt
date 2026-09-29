package com.efm.filemanager.domain.model

import android.net.Uri

data class FileEntry(
    val uri: Uri,
    val documentId: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val sourceApp: SourceApp?,
    val tags: List<FileTag> = emptyList(),
    val isFavorite: Boolean = false,
    val favoriteCollectionId: Long? = null,
    val isLocked: Boolean = false,
    val note: String? = null,
)

data class SourceApp(
    val packageName: String,
    val confidence: SourceConfidence,
)

enum class SourceConfidence { EXACT, HEURISTIC }
