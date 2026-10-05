package com.efm.filemanager.data.statistics

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.StatsCacheDao
import com.efm.filemanager.data.local.StatsCacheEntity
import com.efm.filemanager.data.local.StatsCacheLargestFileEntity
import com.efm.filemanager.data.local.StorageSnapshotDao
import com.efm.filemanager.data.local.StorageSnapshotEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.domain.model.category
import com.efm.filemanager.domain.model.toStorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        private val statsCacheDao: StatsCacheDao,
        private val preferencesRepository: PreferencesRepository,
    ) {
        /**
         * Skips the full-tree walk entirely when nothing's changed since the last call: compares
         * the current [PreferencesRepository.changeVersion] against whatever [StatsCacheDao] has
         * cached from the last walk, and only re-walks on a mismatch (or no cache yet). A cache
         * hit still records today's snapshot -- cheap, since the bytes are already in hand.
         */
        suspend fun computeStorageStats(): StorageStats =
            withContext(Dispatchers.IO) {
                val currentVersion = preferencesRepository.changeVersion.first()
                val cachedMeta = statsCacheDao.getMeta()
                val stats =
                    if (cachedMeta != null && cachedMeta.changeVersion == currentVersion) {
                        statsFromCache(cachedMeta)
                    } else {
                        freshStorageStats(currentVersion)
                    }
                recordDailySnapshot(stats)
                stats
            }

        private suspend fun statsFromCache(cachedMeta: StatsCacheEntity): StorageStats {
            val sizeByCategory =
                storageSnapshotDao.getLatestDayEntries().associate { FileCategory.valueOf(it.category) to it.bytes }
            val largestFiles = statsCacheDao.getLargestFiles().map { it.toFileEntry() }
            return StorageStats(cachedMeta.totalSize, cachedMeta.totalFileCount, sizeByCategory, largestFiles)
        }

        private suspend fun freshStorageStats(currentVersion: Long): StorageStats {
            val stats = collectAllFiles().map { it.toDomain() }.toStorageStats()
            val meta = StatsCacheEntity(changeVersion = currentVersion, totalSize = stats.totalSize, totalFileCount = stats.totalFileCount)
            val cachedLargestFiles =
                stats.largestFiles.mapIndexed { rank, file -> StatsCacheLargestFileEntity(rank, file.uri.toString(), file.name, file.size) }
            statsCacheDao.replaceCache(meta, cachedLargestFiles)
            return stats
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

/**
 * A cache-hit [StatsCacheLargestFileEntity] only ever feeds the largest-files widget's own
 * name/size display, so the fields it never cached (directory flag, last-modified, MIME type,
 * source app) get harmless placeholders rather than a real re-lookup.
 */
internal fun StatsCacheLargestFileEntity.toFileEntry(): FileEntry =
    FileEntry(
        uri = Uri.parse(uri),
        documentId = "",
        name = name,
        isDirectory = false,
        size = size,
        lastModified = 0L,
        mimeType = null,
        sourceApp = null,
    )
