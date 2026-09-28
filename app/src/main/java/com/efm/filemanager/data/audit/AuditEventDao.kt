package com.efm.filemanager.data.audit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditEventDao {
    @Insert
    suspend fun insert(event: AuditEventEntity)

    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AuditEventEntity>>
}
