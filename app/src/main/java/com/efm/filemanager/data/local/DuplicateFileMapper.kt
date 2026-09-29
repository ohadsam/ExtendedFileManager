package com.efm.filemanager.data.local

import android.net.Uri
import com.efm.filemanager.domain.model.DuplicateGroup
import com.efm.filemanager.domain.model.FileEntry

fun FileEntryEntity.toDuplicateEntity(hash: String): DuplicateFileEntity =
    DuplicateFileEntity(
        uri = uri,
        hash = hash,
        parentUri = parentUri,
        documentId = documentId,
        name = name,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
    )

fun DuplicateFileEntity.toDomain(): FileEntry =
    FileEntry(
        uri = Uri.parse(uri),
        documentId = documentId,
        name = name,
        isDirectory = false,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        sourceApp = null,
    )

fun List<DuplicateFileEntity>.toDuplicateGroups(): List<DuplicateGroup> =
    groupBy { it.hash }.map { (hash, entities) -> DuplicateGroup(hash, entities.first().size, entities.map { it.toDomain() }) }
