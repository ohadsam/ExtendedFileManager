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
