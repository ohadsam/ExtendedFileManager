package com.efm.filemanager.data.statistics

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.StorageSnapshotDao
import com.efm.filemanager.data.local.StorageSnapshotEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.domain.model.category
import com.efm.filemanager.domain.model.toStorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
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
        private val storageSnapshotDao: StorageSnapshotDao,
    ) {
        suspend fun computeStorageStats(): StorageStats =
            withContext(Dispatchers.IO) {
                val stats = collectAllFiles().map { it.toDomain() }.toStorageStats()
                recordDailySnapshot(stats)
                stats
            }

        /**
         * Phase 18's trend sparkline -- one point per day this ever ran, forward-built from
         * whenever this phase lands rather than backfilled (Android has no retroactive history
         * of past storage state to query). Fewer than two points means "not enough data yet,"
         * same honesty Phase 10 already applies to its own proxy signals.
         */
        fun observeDailyTotalBytes(): Flow<List<Long>> =
            storageSnapshotDao.observeDailyTotals().map { totals -> totals.map { it.totalBytes } }

        private suspend fun recordDailySnapshot(stats: StorageStats) {
            val day = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)
            val entries = stats.sizeByCategory.map { (category, bytes) -> StorageSnapshotEntity(day, category.name, bytes) }
            storageSnapshotDao.replaceDay(day, entries)
        }

        /**
         * Phase 18's drill-down from the by-type/largest-files widgets -- every file across
         * every granted tree, optionally narrowed to one [FileCategory], sorted largest-first
         * (the one sort these widgets' own cards already imply). [category] null means "every
         * file," used by the largest-files widget's own drill-down.
         */
        suspend fun filesByCategory(category: FileCategory?): List<FileEntry> =
            withContext(Dispatchers.IO) {
                val allFiles = collectAllFiles().map { it.toDomain() }
                val filtered = if (category == null) allFiles else allFiles.filter { it.category() == category }
                filtered.sortedByDescending { it.size }
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
