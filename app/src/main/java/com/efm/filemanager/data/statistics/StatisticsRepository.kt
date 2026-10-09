package com.efm.filemanager.data.statistics

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.StatsCacheDao
import com.efm.filemanager.data.local.StatsCacheEntity
import com.efm.filemanager.data.local.StatsCacheFolderDao
import com.efm.filemanager.data.local.StatsCacheFolderEntity
import com.efm.filemanager.data.local.StatsCacheLargestFileEntity
import com.efm.filemanager.data.local.StatsCacheRecentFileEntity
import com.efm.filemanager.data.local.StorageSnapshotDao
import com.efm.filemanager.data.local.StorageSnapshotEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FolderFileCount
import com.efm.filemanager.domain.model.GlobalFilesSort
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

private const val TOP_FOLDERS_COUNT = 5

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
        private val statsCacheFolderDao: StatsCacheFolderDao,
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
            val recentlyModifiedFiles = statsCacheDao.getRecentFiles().map { it.toFileEntry() }
            val mostPopulatedFolders = statsCacheFolderDao.getFolders().map { FolderFileCount(it.uri, it.name, it.fileCount) }
            return StorageStats(
                totalSize = cachedMeta.totalSize,
                totalFileCount = cachedMeta.totalFileCount,
                sizeByCategory = sizeByCategory,
                largestFiles = largestFiles,
                recentlyModifiedFiles = recentlyModifiedFiles,
                mostPopulatedFolders = mostPopulatedFolders,
            )
        }

        private suspend fun freshStorageStats(currentVersion: Long): StorageStats {
            val walk = walkAllFiles()
            val baseStats = walk.files.map { it.toDomain() }.toStorageStats()
            val mostPopulatedFolders =
                walk.files.topFolderCounts(TOP_FOLDERS_COUNT).map { (parentUri, count) ->
                    FolderFileCount(parentUri, walk.folderNames[parentUri] ?: parentUri, count)
                }
            val stats = baseStats.copy(mostPopulatedFolders = mostPopulatedFolders)
            val meta = StatsCacheEntity(changeVersion = currentVersion, totalSize = stats.totalSize, totalFileCount = stats.totalFileCount)
            val cachedLargestFiles =
                stats.largestFiles.mapIndexed { rank, file -> StatsCacheLargestFileEntity(rank, file.uri.toString(), file.name, file.size) }
            val cachedRecentFiles =
                stats.recentlyModifiedFiles.mapIndexed { rank, file ->
                    StatsCacheRecentFileEntity(rank, file.uri.toString(), file.name, file.lastModified)
                }
            val cachedFolders =
                mostPopulatedFolders.mapIndexed { rank, folder -> StatsCacheFolderEntity(rank, folder.uri, folder.name, folder.fileCount) }
            writeCache(meta, cachedLargestFiles, cachedRecentFiles, cachedFolders)
            return stats
        }

        /**
         * Not wrapped in a single DB transaction -- [StatsCacheDao] and [StatsCacheFolderDao] are
         * separate interfaces (split apart to stay under detekt's `TooManyFunctions` threshold),
         * and this cache is only a display/perf optimization, never the source of truth (a stale
         * or partially-written cache just gets overwritten on the next walk), so that's an
         * acceptable tradeoff rather than reaching for `RoomDatabase.withTransaction`.
         */
        private suspend fun writeCache(
            meta: StatsCacheEntity,
            largestFiles: List<StatsCacheLargestFileEntity>,
            recentFiles: List<StatsCacheRecentFileEntity>,
            folders: List<StatsCacheFolderEntity>,
        ) {
            statsCacheDao.insertMeta(meta)
            statsCacheDao.clearLargestFiles()
            statsCacheDao.insertLargestFiles(largestFiles)
            statsCacheDao.clearRecentFiles()
            statsCacheDao.insertRecentFiles(recentFiles)
            statsCacheFolderDao.clearFolders()
            statsCacheFolderDao.insertFolders(folders)
        }

        /**
         * Phase 18's trend sparkline -- one point per day this ever ran, forward-built from
         * whenever this phase lands rather than backfilled (Android has no retroactive history
         * of past storage state to query). Fewer than two points means "not enough data yet,"
         * same honesty Phase 10 already applies to its own proxy signals.
         */
        fun observeDailyTotalBytes(): Flow<List<Long>> =
            storageSnapshotDao.observeDailyTotals().map { totals -> totals.map { it.totalBytes } }

        /**
         * The second trend chart the original spec called for, alongside [observeDailyTotalBytes]'s
         * total-bytes-over-time: one [FileCategory]'s own history is small-multiples material, not
         * one combined chart -- all seven categories visible at once would need the stricter
         * all-pairs color gate the by-type breakdown widget's own palette was deliberately never
         * validated for (that's the same reason that widget is a stacked bar, not a donut). A lone
         * category drawn by itself, one color at a time, needs no pairwise distinction at all.
         */
        fun observeCategoryTrends(): Flow<Map<FileCategory, List<Long>>> = storageSnapshotDao.observeAll().map { it.toCategoryTrends() }

        private suspend fun recordDailySnapshot(stats: StorageStats) {
            val day = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)
            val entries = stats.sizeByCategory.map { (category, bytes) -> StorageSnapshotEntity(day, category.name, bytes) }
            storageSnapshotDao.replaceDay(day, entries)
        }

        /**
         * Phase 18's drill-down from the by-type/largest-files/recently-modified widgets --
         * every file across every granted tree, optionally narrowed to one [FileCategory],
         * ordered by [sort] (the one order each widget's own card already implies). [category]
         * null means "every file," used by the largest-files and recently-modified widgets'
         * own drill-downs.
         */
        suspend fun filesByCategory(
            category: FileCategory?,
            sort: GlobalFilesSort = GlobalFilesSort.SIZE,
        ): List<FileEntry> =
            withContext(Dispatchers.IO) {
                val allFiles = collectAllFiles().map { it.toDomain() }
                val filtered = if (category == null) allFiles else allFiles.filter { it.category() == category }
                when (sort) {
                    GlobalFilesSort.SIZE -> filtered.sortedByDescending { it.size }
                    GlobalFilesSort.RECENT -> filtered.sortedByDescending { it.lastModified }
                }
            }

        private suspend fun collectAllFiles(): List<FileEntryEntity> = walkAllFiles().files

        /**
         * [FileWalkResult.folderNames] exists purely for the most-populated-folders widget: every
         * non-root folder's display name is already in hand from its own entity before this walk
         * ever recurses into it, so the only *new* SAF lookup this widget needs is one
         * [DocumentTreeRepository.folderDisplayName] call per granted root (never returned as a
         * child entity itself).
         */
        private suspend fun walkAllFiles(): FileWalkResult {
            val out = mutableListOf<FileEntryEntity>()
            val folderNames = mutableMapOf<String, String>()
            documentTreeAccessManager.grantedTreeUris().forEach { root ->
                folderNames[root.toString()] = documentTreeRepository.folderDisplayName(root) ?: root.toString()
                collectFiles(root, out, folderNames)
            }
            return FileWalkResult(out, folderNames)
        }

        private suspend fun collectFiles(
            folderUri: Uri,
            out: MutableList<FileEntryEntity>,
            folderNames: MutableMap<String, String>,
        ) {
            documentTreeRepository.listChildrenFromSaf(folderUri).forEach { entity ->
                if (entity.isDirectory) {
                    folderNames[entity.uri] = entity.name
                    collectFiles(Uri.parse(entity.uri), out, folderNames)
                } else {
                    out.add(entity)
                }
            }
        }
    }

private data class FileWalkResult(
    val files: List<FileEntryEntity>,
    val folderNames: Map<String, String>,
)

/**
 * Pure (operates only on [FileEntryEntity]'s plain-string fields, no Android types) -- the
 * receiver list already holds only files, never directories (see [StatisticsRepository.collectFiles]),
 * so grouping by [FileEntryEntity.parentUri] directly counts a folder's immediate file count, no
 * extra filter needed.
 */
internal fun List<FileEntryEntity>.topFolderCounts(topCount: Int): List<Pair<String, Int>> =
    groupBy { it.parentUri }
        .mapValues { (_, files) -> files.size }
        .entries
        .sortedByDescending { it.value }
        .take(topCount)
        .map { it.key to it.value }

/**
 * Pure (plain string/long fields only) -- groups [StorageSnapshotEntity.category] (stored as
 * [FileCategory]'s own name) back into its enum, dropping any row from a since-removed category
 * name rather than crashing on it. Each category's own list stays in the day-ascending order the
 * query already returns, one entry per day that category was recorded.
 */
internal fun List<StorageSnapshotEntity>.toCategoryTrends(): Map<FileCategory, List<Long>> =
    groupBy { it.category }
        .mapNotNull { (categoryName, rows) ->
            val category = runCatching { FileCategory.valueOf(categoryName) }.getOrNull() ?: return@mapNotNull null
            category to rows.map { it.bytes }
        }
        .toMap()

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

/** Same cache-hit placeholder shape as [StatsCacheLargestFileEntity.toFileEntry], for the recently-modified widget's cache. */
internal fun StatsCacheRecentFileEntity.toFileEntry(): FileEntry =
    FileEntry(
        uri = Uri.parse(uri),
        documentId = "",
        name = name,
        isDirectory = false,
        size = 0L,
        lastModified = lastModified,
        mimeType = null,
        sourceApp = null,
    )
