package com.efm.filemanager.data.duplicates

import android.content.Context
import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.data.local.DuplicateFileDao
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.toDuplicateEntity
import com.efm.filemanager.data.local.toDuplicateGroups
import com.efm.filemanager.data.metadata.FileMetadataRepository
import com.efm.filemanager.data.metadata.enrich
import com.efm.filemanager.domain.model.DuplicateGroup
import com.efm.filemanager.domain.model.FileEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Two-stage duplicate finder over every granted tree -- see docs/PLAN.md Phase 6. Stage one
 * (cheap): group by file size, since files of different sizes can never be identical. Stage
 * two (still cheap): hash only the first few KB of each same-size candidate to eliminate most
 * false positives without reading whole files. Stage three (expensive, so it only runs on
 * survivors of stage two): a full streaming SHA-256 to actually confirm content equality.
 * [scan] always rebuilds from scratch -- simpler and safer than patching stale results in
 * place, matching the same trade-off Phase 5's search index makes.
 */
class DuplicateScanRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val documentTreeAccessManager: DocumentTreeAccessManager,
        private val documentTreeRepository: DocumentTreeRepository,
        private val duplicateFileDao: DuplicateFileDao,
        private val fileOperationsRepository: FileOperationsRepository,
        private val fileMetadataRepository: FileMetadataRepository,
    ) {
        fun observeGroups(): Flow<List<DuplicateGroup>> =
            combine(duplicateFileDao.observeAll(), fileMetadataRepository.snapshot) { entities, snapshot ->
                entities.toDuplicateGroups().map { group -> group.copy(files = group.files.map { it.enrich(snapshot) }) }
            }

        suspend fun scan(onProgress: (ScanProgress) -> Unit): Int =
            withContext(Dispatchers.IO) {
                val allFiles = collectAllFiles()
                val sizeCandidates = candidateSizeGroups(allFiles).flatten()
                val partialGroups = hashAndGroup(sizeCandidates, ScanPhase.COMPARING, onProgress) { uri -> partialHash(context, uri) }
                val fullCandidates = partialGroups.flatMap { (_, files) -> files }
                val finalGroups = hashAndGroup(fullCandidates, ScanPhase.VERIFYING, onProgress) { uri -> fullHash(context, uri) }
                persistResults(finalGroups)
                finalGroups.sumOf { (_, files) -> files.size }
            }

        suspend fun deleteFile(entry: FileEntry): Result<Unit> {
            val stored =
                duplicateFileDao.getByUri(entry.uri.toString())
                    ?: return Result.failure(IllegalStateException("Not in the duplicate results"))
            return fileOperationsRepository.delete(entry, Uri.parse(stored.parentUri)).map {
                duplicateFileDao.deleteByUri(entry.uri.toString())
            }
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

        private fun hashAndGroup(
            candidates: List<FileEntryEntity>,
            phase: ScanPhase,
            onProgress: (ScanProgress) -> Unit,
            hashOf: (Uri) -> String,
        ): List<Pair<String, List<FileEntryEntity>>> {
            val hashed =
                candidates.mapIndexed { index, entity ->
                    onProgress(ScanProgress(phase, index + 1, candidates.size))
                    entity to hashOf(Uri.parse(entity.uri))
                }
            return groupDuplicateCandidates(hashed)
        }

        private suspend fun persistResults(groups: List<Pair<String, List<FileEntryEntity>>>) {
            val entries = groups.flatMap { (hash, files) -> files.map { file -> file.toDuplicateEntity(hash) } }
            duplicateFileDao.replaceAll(entries)
        }
    }
