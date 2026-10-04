package com.efm.filemanager.data.statistics

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.domain.model.toStorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Phase 18's storage-usage widget needs a full-tree, every-file size snapshot, and nothing else
 * in the app already maintains one: Phase 1's file-index cache only covers folders the user has
 * actually browsed into, and Phase 5's search index is only as fresh as its last explicit
 * rebuild. This walk is deliberately the lightest of this app's three full-tree walks -- no
 * hashing (Phase 6), no heuristic checks (Phase 10), just reading each file's already-known
 * size -- same traversal shape as [com.efm.filemanager.data.duplicates.DuplicateScanRepository].
 */
class StatisticsRepository
    @Inject
    constructor(
        private val documentTreeAccessManager: DocumentTreeAccessManager,
        private val documentTreeRepository: DocumentTreeRepository,
    ) {
        suspend fun computeStorageStats(): StorageStats =
            withContext(Dispatchers.IO) {
                collectAllFiles().map { it.toDomain() }.toStorageStats()
            }

        private suspend fun collectAllFiles(): List<FileEntryEntity> {
            val out = mutableListOf<FileEntryEntity>()
            documentTreeAccessManager.grantedTreeUris().forEach { root -> collectFiles(root, out) }
            return out
        }

        private suspend fun collectFiles(
            folderUri: Uri,
            out: MutableList<FileEntryEntity>,
        ) {
            documentTreeRepository.listChildrenFromSaf(folderUri).forEach { entity ->
                if (entity.isDirectory) collectFiles(Uri.parse(entity.uri), out) else out.add(entity)
            }
        }
    }
