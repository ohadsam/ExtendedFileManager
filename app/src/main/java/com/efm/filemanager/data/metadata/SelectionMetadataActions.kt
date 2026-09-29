package com.efm.filemanager.data.metadata

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry
import javax.inject.Inject

/**
 * The tag/favorite/lock quick-actions shared by every screen's selection bar (Browse, Search,
 * Duplicates, Preview -- docs/PLAN.md Phase 9), so this logic lives once instead of once per
 * screen's ViewModel. Bulk favorite/lock use "any not yet in the target state -> apply to all,
 * else clear all" toggle semantics, the common bulk-toggle convention; bulk favoriting always
 * goes to the root (uncategorized) collection -- assigning a specific collection stays a
 * single-file action from the file-details view.
 */
class SelectionMetadataActions
    @Inject
    constructor(
        private val tagRepository: TagRepository,
        private val favoriteRepository: FavoriteRepository,
        private val fileFlagsRepository: FileFlagsRepository,
    ) {
        suspend fun applyTag(
            fileUris: List<Uri>,
            tagId: Long,
        ) = tagRepository.addTagToFiles(fileUris, tagId)

        suspend fun toggleFavorite(entries: List<FileEntry>) {
            val toFavorite = entries.filterNot { it.isFavorite }
            if (toFavorite.isNotEmpty()) {
                favoriteRepository.setFavorite(toFavorite.map { it.uri }, collectionId = null)
            } else {
                entries.forEach { entry -> favoriteRepository.removeFavorite(entry.uri) }
            }
        }

        suspend fun toggleLock(entries: List<FileEntry>) {
            val shouldLock = entries.any { !it.isLocked }
            fileFlagsRepository.setLocked(entries.map { it.uri }, shouldLock)
        }

        suspend fun unlock(fileUris: List<Uri>) = fileFlagsRepository.setLocked(fileUris, locked = false)
    }
