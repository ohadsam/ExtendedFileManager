package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageRecommendationDao {
    @Query("SELECT * FROM storage_recommendations ORDER BY size DESC")
    fun observeAll(): Flow<List<StorageRecommendationEntity>>

    @Query("SELECT * FROM storage_recommendations WHERE uri = :uri AND category = :category")
    suspend fun get(
        uri: String,
        category: String,
    ): StorageRecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StorageRecommendationEntity>)

    @Query("DELETE FROM storage_recommendations")
    suspend fun clear()

    @Query("DELETE FROM storage_recommendations WHERE uri = :uri AND category = :category")
    suspend fun delete(
        uri: String,
        category: String,
    )

    @Query("DELETE FROM storage_recommendations WHERE uri = :uri")
    suspend fun deleteAllCategoriesForUri(uri: String)

    @Transaction
    suspend fun replaceAll(entries: List<StorageRecommendationEntity>) {
        clear()
        insertAll(entries)
    }
}
