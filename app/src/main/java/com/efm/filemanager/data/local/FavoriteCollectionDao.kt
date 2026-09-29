package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCollectionDao {
    @Query("SELECT * FROM favorite_collections ORDER BY sortOrder")
    fun observeCollections(): Flow<List<FavoriteCollectionEntity>>

    @Query("SELECT * FROM favorite_collections WHERE parentId IS :parentId ORDER BY sortOrder")
    suspend fun collectionsInParent(parentId: Long?): List<FavoriteCollectionEntity>

    @Query("SELECT MAX(sortOrder) FROM favorite_collections WHERE parentId IS :parentId")
    suspend fun maxSortOrder(parentId: Long?): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: FavoriteCollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: FavoriteCollectionEntity)

    @Query("DELETE FROM favorite_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("UPDATE favorites SET collectionId = NULL WHERE collectionId = :id")
    suspend fun orphanFavoritesOf(id: Long)

    @Query("UPDATE favorite_collections SET parentId = NULL WHERE parentId = :id")
    suspend fun orphanChildCollectionsOf(id: Long)
}
