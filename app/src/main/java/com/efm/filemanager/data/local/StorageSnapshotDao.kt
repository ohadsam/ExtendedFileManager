package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** One day's total bytes across every category -- the trend sparkline's actual data points. */
data class DailyTotal(
    val day: Long,
    val totalBytes: Long,
)

@Dao
interface StorageSnapshotDao {
    @Query("SELECT day, SUM(bytes) AS totalBytes FROM storage_snapshots GROUP BY day ORDER BY day ASC")
    fun observeDailyTotals(): Flow<List<DailyTotal>>

    /**
     * The per-category rows of the most recently recorded day -- Phase 18's cache-hit path
     * rebuilds `StorageStats.sizeByCategory` from these instead of re-walking the tree.
     */
    @Query("SELECT * FROM storage_snapshots WHERE day = (SELECT MAX(day) FROM storage_snapshots)")
    suspend fun getLatestDayEntries(): List<StorageSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StorageSnapshotEntity>)

    @Query("DELETE FROM storage_snapshots WHERE day = :day")
    suspend fun deleteForDay(day: Long)

    @Transaction
    suspend fun replaceDay(
        day: Long,
        entries: List<StorageSnapshotEntity>,
    ) {
        deleteForDay(day)
        insertAll(entries)
    }
}
