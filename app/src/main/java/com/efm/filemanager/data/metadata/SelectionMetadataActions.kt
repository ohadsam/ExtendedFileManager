package com.efm.filemanager.data.metadata

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry
import javax.inject.Inject

/**
 * The tag/favorite/lock quick-actions shared by every screen's selection bar (Browse, Search,
 * Duplicates, Preview -- docs/PLAN.md Phase 9), so this logic lives once instead of once per
 * screen's ViewModel. Bulk favorite/lock use "any not yet in the target state -> apply to all,
 * else clear all" toggle semantics, the common bulk-toggle convention. [willFavorite] tells the
 * UI which branch a toggle will take, so it can offer a collection picker only when the action
 * is actually going to favorite something (never when it's about to remove).
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

        fun willFavorite(entries: List<FileEntry>): Boolean = entries.any { !it.isFavorite }

        /** Direct toggle with no collection choice -- used when [willFavorite] is false (the action only removes). */
        suspend fun toggleFavorite(entries: List<FileEntry>) {
            if (willFavorite(entries)) {
                favoriteRepository.setFavorite(entries.map { it.uri }, collectionId = null)
            } else {
                entries.forEach { entry -> favoriteRepository.removeFavorite(entry.uri) }
            }
        }

        /** Favorites every entry into [collectionId] -- the picker-confirmed path when [willFavorite] is true. */
        suspend fun favoriteInto(
            entries: List<FileEntry>,
            collectionId: Long?,
        ) = favoriteRepository.setFavorite(entries.map { it.uri }, collectionId)

        suspend fun toggleLock(entries: List<FileEntry>) {
            val shouldLock = entries.any { !it.isLocked }
            fileFlagsRepository.setLocked(entries.map { it.uri }, shouldLock)
        }

        suspend fun unlock(fileUris: List<Uri>) = fileFlagsRepository.setLocked(fileUris, locked = false)
    }
