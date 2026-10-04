package com.efm.filemanager.data.local

import androidx.room.Entity

/**
 * Phase 18's storage-trend history -- one row per (day, category), appended whenever
 * [com.efm.filemanager.data.statistics.StatisticsRepository.computeStorageStats] runs, so the
 * trend sparkline is built forward from whenever this phase lands rather than backfilled. [day]
 * is a whole-day bucket (epoch millis / one day), not a timestamp, so recomputing more than once
 * the same day just overwrites that day's rows instead of accumulating duplicates.
 */
@Entity(tableName = "storage_snapshots", primaryKeys = ["day", "category"])
data class StorageSnapshotEntity(
    val day: Long,
    val category: String,
    val bytes: Long,
)
