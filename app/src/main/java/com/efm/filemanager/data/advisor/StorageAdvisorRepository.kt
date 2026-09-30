package com.efm.filemanager.data.advisor

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.efm.filemanager.R
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.StorageRecommendationDao
import com.efm.filemanager.data.local.StorageRecommendationEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.data.local.toRecommendationEntity
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.RecommendationReason
import com.efm.filemanager.domain.model.StorageRecommendation
import com.efm.filemanager.domain.model.StorageRecommendationCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** [FileEntry.uri] scheme for EFM's own app-private cache -- it never came from SAF, so it needs its own deletion path. */
private const val APP_CACHE_URI_SCHEME = "file"

/** What one scan pass needs at every folder it visits -- bundled so the walk's own functions stay under a handful of params. */
private data class AdvisorScanContext(
    val isPackageInstalled: (String) -> Boolean,
    val lastOpenedAtByUri: Map<String, Long>,
    val now: Long,
    val thresholds: LargeFileThresholds,
    val onProgress: (AdvisorScanProgress) -> Unit,
)

private class AdvisorScanAccumulator {
    val results = mutableListOf<StorageRecommendationEntity>()
    var filesScanned = 0
}

/**
 * Walks every granted tree once, same shape as [com.efm.filemanager.data.duplicates.DuplicateScanRepository] --
 * see docs/PLAN.md Phase 10. Recommendations only: nothing here ever deletes without the caller
 * explicitly confirming via [deleteRecommendation].
 */
class StorageAdvisorRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val documentTreeAccessManager: DocumentTreeAccessManager,
        private val documentTreeRepository: DocumentTreeRepository,
        private val storageRecommendationDao: StorageRecommendationDao,
        private val fileOperationsRepository: FileOperationsRepository,
        private val advisorScanSettings: AdvisorScanSettings,
    ) {
        fun observeRecommendations(): Flow<List<StorageRecommendation>> =
            storageRecommendationDao.observeAll().map { entities -> entities.mapNotNull { it.toDomain() } }

        suspend fun scan(onProgress: (AdvisorScanProgress) -> Unit): Int =
            withContext(Dispatchers.IO) {
                val scanContext =
                    AdvisorScanContext(
                        isPackageInstalled = ::isPackageInstalled,
                        lastOpenedAtByUri = advisorScanSettings.lastOpenedAtByUri(),
                        now = System.currentTimeMillis(),
                        thresholds = advisorScanSettings.largeFileThresholds(),
                        onProgress = onProgress,
                    )
                val accumulator = AdvisorScanAccumulator()
                documentTreeAccessManager.grantedTreeUris().forEach { root -> visitFolder(root, emptyList(), scanContext, accumulator) }
                appCacheEntity()?.let { cache ->
                    accumulator.results += cache.toRecommendationEntity(StorageRecommendationCategory.JUNK, RecommendationReason.APP_CACHE)
                }
                storageRecommendationDao.replaceAll(accumulator.results)
                accumulator.results.size
            }

        suspend fun deleteRecommendation(recommendation: StorageRecommendation): Result<Unit> {
            val uri = recommendation.entry.uri.toString()
            if (recommendation.entry.uri.scheme == APP_CACHE_URI_SCHEME) {
                return deleteAppCache().also { result -> if (result.isSuccess) storageRecommendationDao.deleteAllCategoriesForUri(uri) }
            }
            val stored = storageRecommendationDao.get(uri, recommendation.category.name)
            return if (stored == null) {
                Result.failure(IllegalStateException("Not in the current recommendations"))
            } else {
                fileOperationsRepository.delete(recommendation.entry, Uri.parse(stored.parentUri)).map {
                    storageRecommendationDao.deleteAllCategoriesForUri(uri)
                }
            }
        }

        suspend fun dismiss(recommendation: StorageRecommendation) {
            storageRecommendationDao.delete(recommendation.entry.uri.toString(), recommendation.category.name)
        }

        /** Deletes a staged file directly -- it may or may not still be a live recommendation, so any matching row is cleared too. */
        suspend fun deleteStagedEntry(entry: FileEntry): Result<Unit> {
            val uri = entry.uri.toString()
            if (entry.uri.scheme == APP_CACHE_URI_SCHEME) {
                return deleteAppCache().also { result -> if (result.isSuccess) storageRecommendationDao.deleteAllCategoriesForUri(uri) }
            }
            val parentUri = documentTreeRepository.parentUriOf(entry.uri)
            return if (parentUri == null) {
                Result.failure(IllegalStateException("Unknown parent for $uri"))
            } else {
                fileOperationsRepository.delete(entry, parentUri).map {
                    storageRecommendationDao.deleteAllCategoriesForUri(uri)
                }
            }
        }

        /** Lists [folderUri]'s children once; an empty result flags [selfEntity] itself as [RecommendationReason.EMPTY_FOLDER]. */
        private suspend fun visitFolder(
            folderUri: Uri,
            ancestorNames: List<String>,
            scanContext: AdvisorScanContext,
            accumulator: AdvisorScanAccumulator,
            selfEntity: FileEntryEntity? = null,
        ) {
            val children = documentTreeRepository.listChildrenFromSaf(folderUri)
            if (children.isEmpty()) {
                if (selfEntity != null) {
                    accumulator.results +=
                        selfEntity.toRecommendationEntity(StorageRecommendationCategory.JUNK, RecommendationReason.EMPTY_FOLDER)
                }
                return
            }
            children.forEach { child ->
                accumulator.filesScanned++
                scanContext.onProgress(AdvisorScanProgress(accumulator.filesScanned))
                if (child.isDirectory) {
                    visitDirectory(child, ancestorNames, scanContext, accumulator)
                } else {
                    visitFile(child, scanContext, accumulator)
                }
            }
        }

        private suspend fun visitDirectory(
            folder: FileEntryEntity,
            ancestorNames: List<String>,
            scanContext: AdvisorScanContext,
            accumulator: AdvisorScanAccumulator,
        ) {
            if (isOrphanedAppMediaFolder(ancestorNames, folder.name, scanContext.isPackageInstalled)) {
                accumulator.results +=
                    folder.toRecommendationEntity(StorageRecommendationCategory.JUNK, RecommendationReason.ORPHANED_APP_FOLDER)
                return
            }
            visitFolder(Uri.parse(folder.uri), ancestorNames + folder.name, scanContext, accumulator, selfEntity = folder)
        }

        private fun visitFile(
            file: FileEntryEntity,
            scanContext: AdvisorScanContext,
            accumulator: AdvisorScanAccumulator,
        ) {
            matchesTemporaryPattern(file.name)?.let { pattern ->
                val recommendation =
                    file.toRecommendationEntity(StorageRecommendationCategory.TEMPORARY, RecommendationReason.TEMP_FILE_PATTERN, pattern)
                accumulator.results += recommendation
            }
            val lastOpenedAt = scanContext.lastOpenedAtByUri[file.uri]
            largeUnusedReason(file.size, file.lastModified, lastOpenedAt, scanContext.now, scanContext.thresholds)?.let { reason ->
                accumulator.results += file.toRecommendationEntity(StorageRecommendationCategory.LARGE_UNUSED, reason)
            }
        }

        /**
         * A specific-package point check rather than [PackageManager.getInstalledApplications] --
         * simpler to write against an arbitrary, discovered-at-scan-time package name than
         * enumerating every installed app up front. The manifest declares QUERY_ALL_PACKAGES
         * (file managers are one of Google Play's documented exceptions to the API 30+
         * package-visibility restriction) specifically so this lookup sees every installed app,
         * not just the ones EFM would otherwise be visible to.
         */
        private fun isPackageInstalled(packageName: String): Boolean =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getApplicationInfo(packageName, 0)
                }
            }.isSuccess

        /**
         * EFM's own app-private cache never came from SAF -- there's exactly one of it, it's
         * always at a known path, and [context] already has direct java.io.File access to it, so
         * this doesn't need the granted-tree walk [visitFolder] does. Returns null when empty,
         * same as any other recommendation category that found nothing.
         */
        private fun appCacheEntity(): FileEntryEntity? {
            val cacheDir = context.cacheDir
            val size = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            if (size <= 0) return null
            return FileEntryEntity(
                uri = Uri.fromFile(cacheDir).toString(),
                parentUri = "",
                documentId = "app_cache",
                name = context.getString(R.string.storage_advisor_app_cache_name),
                isDirectory = true,
                size = size,
                lastModified = cacheDir.lastModified(),
                mimeType = null,
            )
        }

        /** Clears EFM's own cache directly via [java.io.File], then recreates it -- Android expects it to keep existing. */
        private fun deleteAppCache(): Result<Unit> =
            runCatching {
                check(context.cacheDir.deleteRecursively()) { "Could not fully clear the app cache" }
                context.cacheDir.mkdirs()
                Unit
            }
    }
