package com.efm.filemanager.data.local

import androidx.room.Entity

@Entity(tableName = "file_flags", primaryKeys = ["fileUri"])
data class FileFlagsEntity(
    val fileUri: String,
    val locked: Boolean = false,
    val note: String? = null,
)
