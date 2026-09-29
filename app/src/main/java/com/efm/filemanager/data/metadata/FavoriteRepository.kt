package com.efm.filemanager.data.metadata

import android.net.Uri
import androidx.room.withTransaction
import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.FavoriteCollectionDao
import com.efm.filemanager.data.local.FavoriteCollectionEntity
import com.efm.filemanager.data.local.FavoriteDao
import com.efm.filemanager.data.local.FavoriteEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.domain.model.FavoriteCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class MoveDirection { UP, DOWN }

/**
 * The favorites tree: nested collections (self-referencing, [FavoriteCollection.parentId])
 * plus which files/folders are favorited into which collection -- per docs/PLAN.md Phase 9.
 * Reordering is explicit move-up/move-down rather than drag-and-drop: simpler to get right
 * and just as usable for the handful of siblings a collection realistically holds.
 */
class FavoriteRepository
    @Inject
    constructor(
        private val favoriteDao: FavoriteDao,
        private val collectionDao: FavoriteCollectionDao,
        private val database: EfmDatabase,
    ) {
        val collections: Flow<List<FavoriteCollection>> = collectionDao.observeCollections().map { it.map { c -> c.toDomain() } }

        val favoritesByFileUri: Flow<Map<String, FavoriteEntity>> =
            favoriteDao.observeAll().map { favorites -> favorites.associateBy { it.fileUri } }

        suspend fun createCollection(
            name: String,
            parentId: Long?,
        ): Long {
            val nextOrder = (collectionDao.maxSortOrder(parentId) ?: -1) + 1
            return collectionDao.insertCollection(FavoriteCollectionEntity(name = name, parentId = parentId, sortOrder = nextOrder))
        }

        suspend fun renameCollection(
            collection: FavoriteCollection,
            name: String,
        ) = collectionDao.updateCollection(collection.copy(name = name).toEntity())

        /** Deleting a collection never deletes what was inside it -- favorites and child collections move to the root. */
        suspend fun deleteCollection(collection: FavoriteCollection) {
            database.withTransaction {
                collectionDao.orphanFavoritesOf(collection.id)
                collectionDao.orphanChildCollectionsOf(collection.id)
                collectionDao.deleteCollection(collection.id)
            }
        }

        suspend fun moveCollection(
            collection: FavoriteCollection,
            direction: MoveDirection,
        ) {
            val siblings = collectionDao.collectionsInParent(collection.parentId)
            val index = siblings.indexOfFirst { it.id == collection.id }
            val swapIndex = if (direction == MoveDirection.UP) index - 1 else index + 1
            if (index < 0 || swapIndex !in siblings.indices) return
            val current = siblings[index]
            val swapWith = siblings[swapIndex]
            collectionDao.updateCollection(current.copy(sortOrder = swapWith.sortOrder))
            collectionDao.updateCollection(swapWith.copy(sortOrder = current.sortOrder))
        }

        suspend fun setFavorite(
            fileUris: List<Uri>,
            collectionId: Long?,
        ) {
            val now = System.currentTimeMillis()
            fileUris.forEach { uri ->
                favoriteDao.setFavorite(FavoriteEntity(fileUri = uri.toString(), collectionId = collectionId, addedAt = now))
            }
        }

        suspend fun removeFavorite(fileUri: Uri) = favoriteDao.removeFavorite(fileUri.toString())
    }

private fun FavoriteCollection.toEntity(): FavoriteCollectionEntity =
    FavoriteCollectionEntity(id = id, name = name, parentId = parentId, sortOrder = sortOrder)
