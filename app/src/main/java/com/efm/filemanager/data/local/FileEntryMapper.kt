package com.efm.filemanager.data.local

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.SourceApp
import com.efm.filemanager.domain.model.SourceConfidence

fun FileEntryEntity.toDomain(): FileEntry =
    FileEntry(
        uri = Uri.parse(uri),
        documentId = documentId,
        name = name,
        isDirectory = isDirectory,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        sourceApp = toSourceApp(),
    )

internal fun FileEntryEntity.toSourceApp(): SourceApp? {
    val packageName = ownerPackageName ?: return null
    val confidence = sourceConfidence?.let { runCatching { SourceConfidence.valueOf(it) }.getOrNull() }
    return confidence?.let { SourceApp(packageName, it) }
}

fun FileEntryEntity.toFtsEntity(): FileEntryFtsEntity = FileEntryFtsEntity(uri = uri, name = name)
