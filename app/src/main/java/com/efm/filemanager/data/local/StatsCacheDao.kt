package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StatsCacheDao {
    @Query("SELECT * FROM stats_cache WHERE id = 0")
    suspend fun getMeta(): StatsCacheEntity?

    @Query("SELECT * FROM stats_cache_largest_files ORDER BY rank ASC")
    suspend fun getLargestFiles(): List<StatsCacheLargestFileEntity>

    @Query("SELECT * FROM stats_cache_recent_files ORDER BY rank ASC")
    suspend fun getRecentFiles(): List<StatsCacheRecentFileEntity>

    @Query("SELECT * FROM stats_cache_folders ORDER BY rank ASC")
    suspend fun getFolders(): List<StatsCacheFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: StatsCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLargestFiles(files: List<StatsCacheLargestFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFiles(files: List<StatsCacheRecentFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<StatsCacheFolderEntity>)

    @Query("DELETE FROM stats_cache_largest_files")
    suspend fun clearLargestFiles()

    @Query("DELETE FROM stats_cache_recent_files")
    suspend fun clearRecentFiles()

    @Query("DELETE FROM stats_cache_folders")
    suspend fun clearFolders()

    @Transaction
    suspend fun replaceCache(
        meta: StatsCacheEntity,
        largestFiles: List<StatsCacheLargestFileEntity>,
        recentFiles: List<StatsCacheRecentFileEntity>,
        folders: List<StatsCacheFolderEntity>,
    ) {
        insertMeta(meta)
        clearLargestFiles()
        insertLargestFiles(largestFiles)
        clearRecentFiles()
        insertRecentFiles(recentFiles)
        clearFolders()
        insertFolders(folders)
    }
}
