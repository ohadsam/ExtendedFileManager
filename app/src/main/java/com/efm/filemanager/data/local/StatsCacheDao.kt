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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: StatsCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLargestFiles(files: List<StatsCacheLargestFileEntity>)

    @Query("DELETE FROM stats_cache_largest_files")
    suspend fun clearLargestFiles()

    @Transaction
    suspend fun replaceCache(
        meta: StatsCacheEntity,
        largestFiles: List<StatsCacheLargestFileEntity>,
    ) {
        insertMeta(meta)
        clearLargestFiles()
        insertLargestFiles(largestFiles)
    }
}
