package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {
    @Query("SELECT * FROM vault_entries ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getById(id: Long): VaultEntryEntity?

    @Insert
    suspend fun insert(entry: VaultEntryEntity): Long

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
