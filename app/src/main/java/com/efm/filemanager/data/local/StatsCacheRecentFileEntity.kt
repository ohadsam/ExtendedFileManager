package com.efm.filemanager.data.local

import androidx.room.Entity

/**
 * One of [StatsCacheEntity]'s cached top-N most-recently-modified files, [rank] 0 being the
 * newest. Same "display cache, not a full FileEntry" shape as [StatsCacheLargestFileEntity].
 */
@Entity(tableName = "stats_cache_recent_files", primaryKeys = ["rank"])
data class StatsCacheRecentFileEntity(
    val rank: Int,
    val uri: String,
    val name: String,
    val lastModified: Long,
)
