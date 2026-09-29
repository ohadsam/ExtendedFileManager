package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileFlagsDao {
    @Query("SELECT * FROM file_flags")
    fun observeAll(): Flow<List<FileFlagsEntity>>

    @Query("SELECT * FROM file_flags WHERE fileUri = :fileUri")
    suspend fun getByUri(fileUri: String): FileFlagsEntity?

    @Query("SELECT * FROM file_flags WHERE stagedAt IS NOT NULL")
    fun observeStaged(): Flow<List<FileFlagsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(flags: FileFlagsEntity)
}
