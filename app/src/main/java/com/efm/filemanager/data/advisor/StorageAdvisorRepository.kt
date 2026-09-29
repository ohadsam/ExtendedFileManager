package com.efm.filemanager.data.advisor

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.StorageRecommendationDao
import com.efm.filemanager.data.local.StorageRecommendationEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.data.local.toRecommendationEntity
import com.efm.filemanager.data.metadata.FileFlagsRepository
import com.efm.filemanager.domain.model.RecommendationReason
import com.efm.filemanager.domain.model.StorageRecommendation
import com.efm.filemanager.domain.model.StorageRecommendationCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** What one scan pass needs at every folder it visits -- bundled so the walk's own functions stay under a handful of params. */
private data class AdvisorScanContext(
    val isPackageInstalled: (String) -> Boolean,
    val lastOpenedAtByUri: Map<String, Long>,
    val now: Long,
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
        private val fileFlagsRepository: FileFlagsRepository,
    ) {
        fun observeRecommendations(): Flow<List<StorageRecommendation>> =
            storageRecommendationDao.observeAll().map { entities -> entities.mapNotNull { it.toDomain() } }

        suspend fun scan(onProgress: (AdvisorScanProgress) -> Unit): Int =
            withContext(Dispatchers.IO) {
                val scanContext =
                    AdvisorScanContext(
                        isPackageInstalled = ::isPackageInstalled,
                        lastOpenedAtByUri = fileFlagsRepository.lastOpenedAtByUriOnce(),
                        now = System.currentTimeMillis(),
                        onProgress = onProgress,
                    )
                val accumulator = AdvisorScanAccumulator()
                documentTreeAccessManager.grantedTreeUris().forEach { root -> visitFolder(root, emptyList(), scanContext, accumulator) }
                storageRecommendationDao.replaceAll(accumulator.results)
                accumulator.results.size
            }

        suspend fun deleteRecommendation(recommendation: StorageRecommendation): Result<Unit> {
            val uri = recommendation.entry.uri.toString()
            val stored =
                storageRecommendationDao.get(uri, recommendation.category.name)
                    ?: return Result.failure(IllegalStateException("Not in the current recommendations"))
            return fileOperationsRepository.delete(recommendation.entry, Uri.parse(stored.parentUri)).map {
                storageRecommendationDao.deleteAllCategoriesForUri(uri)
            }
        }

        suspend fun dismiss(recommendation: StorageRecommendation) {
            storageRecommendationDao.delete(recommendation.entry.uri.toString(), recommendation.category.name)
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
            largeUnusedReason(file.size, file.lastModified, lastOpenedAt, scanContext.now)?.let { reason ->
                accumulator.results += file.toRecommendationEntity(StorageRecommendationCategory.LARGE_UNUSED, reason)
            }
        }

        /**
         * A specific-package point check rather than [PackageManager.getInstalledApplications] --
         * on API 30+ that listing is filtered by package-visibility rules the same way a single
         * lookup is, so there's no accuracy to gain from enumerating everything up front, and a
         * point check needs no version-gated bulk-query permission.
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
    }
