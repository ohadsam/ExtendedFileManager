package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FileEntryDao {

    @Query("SELECT * FROM file_entries WHERE parentUri = :parentUri ORDER BY isDirectory DESC, name COLLATE NOCASE ASC")
    fun observeChildren(parentUri: String): Flow<List<FileEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FileEntryEntity>)

    @Query("DELETE FROM file_entries WHERE parentUri = :parentUri")
    suspend fun deleteChildren(parentUri: String)

    @Transaction
    suspend fun replaceChildren(parentUri: String, entries: List<FileEntryEntity>) {
        deleteChildren(parentUri)
        insertAll(entries)
    }
}
