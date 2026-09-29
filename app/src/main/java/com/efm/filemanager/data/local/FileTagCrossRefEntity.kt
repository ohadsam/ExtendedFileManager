package com.efm.filemanager.data.local

import androidx.room.Entity

@Entity(tableName = "file_tag_cross_refs", primaryKeys = ["fileUri", "tagId"])
data class FileTagCrossRefEntity(
    val fileUri: String,
    val tagId: Long,
)
