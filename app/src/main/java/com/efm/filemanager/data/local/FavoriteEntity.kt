package com.efm.filemanager.data.local

import androidx.room.Entity

@Entity(tableName = "favorites", primaryKeys = ["fileUri"])
data class FavoriteEntity(
    val fileUri: String,
    val collectionId: Long?,
    val addedAt: Long,
)
