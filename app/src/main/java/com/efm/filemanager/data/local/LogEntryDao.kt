package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Insert
    suspend fun insert(entry: LogEntryEntity)

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LogEntryEntity>>

    @Query("DELETE FROM log_entries")
    suspend fun clearAll()

    @Query("DELETE FROM log_entries WHERE timestamp < :cutoffMillis")
    suspend fun purgeOlderThan(cutoffMillis: Long)
}
