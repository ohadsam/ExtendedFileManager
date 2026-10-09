package com.efm.filemanager.data.local

import androidx.room.Entity

/** One of [StatsCacheEntity]'s cached top-N most-populated folders, [rank] 0 being the busiest. [uri] is kept for drill-down. */
@Entity(tableName = "stats_cache_folders", primaryKeys = ["rank"])
data class StatsCacheFolderEntity(
    val rank: Int,
    val uri: String,
    val name: String,
    val fileCount: Int,
)
