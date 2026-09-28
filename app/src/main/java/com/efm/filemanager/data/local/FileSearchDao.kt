package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileSearchDao {
    @Query("SELECT uri FROM file_search_fts WHERE file_search_fts MATCH :query")
    suspend fun matchUris(query: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FileEntryFtsEntity>)

    @Query("DELETE FROM file_search_fts")
    suspend fun clear()
}
