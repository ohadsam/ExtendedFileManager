package com.efm.filemanager.data.local

import androidx.room.Entity

@Entity(tableName = "duplicate_files", primaryKeys = ["uri"])
data class DuplicateFileEntity(
    val uri: String,
    val hash: String,
    val parentUri: String,
    val documentId: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
)
