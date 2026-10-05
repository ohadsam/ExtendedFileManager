package com.efm.filemanager.data.local

import androidx.room.Entity

/**
 * Phase 18's incremental-recompute cache -- a single row (fixed [id]) holding the scalar result
 * of the last full-tree walk [com.efm.filemanager.data.statistics.StatisticsRepository] ran, and
 * the [changeVersion] it was computed at. The next call compares the current change version
 * against this one and, when nothing has moved since, skips the walk entirely and reconstructs
 * [com.efm.filemanager.domain.model.StorageStats] from this row plus [StatsCacheLargestFileEntity]
 * and the most recent [StorageSnapshotEntity] day instead.
 */
@Entity(tableName = "stats_cache", primaryKeys = ["id"])
data class StatsCacheEntity(
    val id: Int = 0,
    val changeVersion: Long,
    val totalSize: Long,
    val totalFileCount: Int,
)
