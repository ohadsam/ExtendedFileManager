package com.efm.filemanager.data.local

import androidx.room.Entity

@Entity(tableName = "file_entries", primaryKeys = ["uri"])
data class FileEntryEntity(
    val uri: String,
    val parentUri: String,
    val documentId: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val ownerPackageName: String?,
    val sourceConfidence: String?,
)
