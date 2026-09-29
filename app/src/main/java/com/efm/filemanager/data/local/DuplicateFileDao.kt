package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DuplicateFileDao {
    @Query("SELECT * FROM duplicate_files ORDER BY hash")
    fun observeAll(): Flow<List<DuplicateFileEntity>>

    @Query("SELECT * FROM duplicate_files WHERE uri = :uri")
    suspend fun getByUri(uri: String): DuplicateFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DuplicateFileEntity>)

    @Query("DELETE FROM duplicate_files")
    suspend fun clear()

    @Query("DELETE FROM duplicate_files WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Transaction
    suspend fun replaceAll(entries: List<DuplicateFileEntity>) {
        clear()
        insertAll(entries)
    }
}
