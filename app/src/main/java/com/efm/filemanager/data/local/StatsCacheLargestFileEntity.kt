package com.efm.filemanager.data.local

import androidx.room.Entity

/**
 * One of [StatsCacheEntity]'s cached top-N largest files, [rank] 0 being the biggest. Only the
 * fields the largest-files widget actually renders (name, size) plus [uri] for drill-down are
 * kept -- this is a display cache, not a full [com.efm.filemanager.domain.model.FileEntry].
 */
@Entity(tableName = "stats_cache_largest_files", primaryKeys = ["rank"])
data class StatsCacheLargestFileEntity(
    val rank: Int,
    val uri: String,
    val name: String,
    val size: Long,
)
