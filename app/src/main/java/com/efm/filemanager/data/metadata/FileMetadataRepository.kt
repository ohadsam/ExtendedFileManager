package com.efm.filemanager.data.metadata

import com.efm.filemanager.data.local.FileEntryDao
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.domain.model.FileEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Combines [TagRepository]/[FavoriteRepository]/[FileFlagsRepository] into one snapshot for enriching FileEntry lists. */
class FileMetadataRepository
    @Inject
    constructor(
        private val tagRepository: TagRepository,
        private val favoriteRepository: FavoriteRepository,
        private val fileFlagsRepository: FileFlagsRepository,
        private val fileEntryDao: FileEntryDao,
    ) {
        val snapshot: Flow<FileMetadataSnapshot> =
            combine(
                tagRepository.tagsByFileUri,
                favoriteRepository.favoritesByFileUri,
                fileFlagsRepository.flagsByFileUri,
            ) { tags, favorites, flags -> FileMetadataSnapshot(tags, favorites, flags) }

        suspend fun snapshotOnce(): FileMetadataSnapshot = snapshot.first()

        /** Resolves every currently favorited file from the local index cache -- Phase 1's cache, not a fresh SAF walk. */
        suspend fun resolveFavoriteEntries(): List<FileEntry> {
            val snap = snapshotOnce()
            val uris = snap.favoritesByFileUri.keys.toList()
            if (uris.isEmpty()) return emptyList()
            return fileEntryDao.getByUris(uris).map { it.toDomain().enrich(snap) }
        }
    }
