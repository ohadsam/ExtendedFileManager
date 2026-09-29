package com.efm.filemanager.data.metadata

import android.net.Uri
import com.efm.filemanager.data.local.FileFlagsDao
import com.efm.filemanager.data.local.FileFlagsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Per-file lock-against-deletion, free-text note, staged-for-deletion, and last-opened-via-EFM -- Phase 9/10's flags. */
class FileFlagsRepository
    @Inject
    constructor(
        private val fileFlagsDao: FileFlagsDao,
    ) {
        val flagsByFileUri: Flow<Map<String, FileFlagsEntity>> =
            fileFlagsDao.observeAll().map { flags -> flags.associateBy { it.fileUri } }

        val stagedByFileUri: Flow<Map<String, FileFlagsEntity>> =
            fileFlagsDao.observeStaged().map { flags -> flags.associateBy { it.fileUri } }

        suspend fun isLocked(fileUri: Uri): Boolean = fileFlagsDao.getByUri(fileUri.toString())?.locked ?: false

        suspend fun setLocked(
            fileUris: List<Uri>,
            locked: Boolean,
        ) {
            fileUris.forEach { uri ->
                val current = fileFlagsDao.getByUri(uri.toString()) ?: FileFlagsEntity(fileUri = uri.toString())
                fileFlagsDao.upsert(current.copy(locked = locked))
            }
        }

        suspend fun setNote(
            fileUri: Uri,
            note: String?,
        ) {
            val current = fileFlagsDao.getByUri(fileUri.toString()) ?: FileFlagsEntity(fileUri = fileUri.toString())
            fileFlagsDao.upsert(current.copy(note = note?.ifBlank { null }))
        }

        /** Marks [fileUris] staged-for-deletion review, skipping any that are locked -- a locked file can't be staged. */
        suspend fun stageForDeletion(fileUris: List<Uri>) {
            val now = System.currentTimeMillis()
            fileUris.forEach { uri ->
                val current = fileFlagsDao.getByUri(uri.toString()) ?: FileFlagsEntity(fileUri = uri.toString())
                if (!current.locked) fileFlagsDao.upsert(current.copy(stagedAt = now))
            }
        }

        suspend fun unstage(fileUris: List<Uri>) {
            fileUris.forEach { uri ->
                val current = fileFlagsDao.getByUri(uri.toString()) ?: return@forEach
                fileFlagsDao.upsert(current.copy(stagedAt = null))
            }
        }

        /** Called whenever the user previews/opens [fileUri] through EFM itself (Phase 7) -- the advisor's secondary "unused" signal. */
        suspend fun recordOpened(fileUri: Uri) {
            val current = fileFlagsDao.getByUri(fileUri.toString()) ?: FileFlagsEntity(fileUri = fileUri.toString())
            fileFlagsDao.upsert(current.copy(lastOpenedAt = System.currentTimeMillis()))
        }

        suspend fun lastOpenedAtByUriOnce(): Map<String, Long> =
            flagsByFileUri.first().mapNotNull { (uri, flags) -> flags.lastOpenedAt?.let { uri to it } }.toMap()
    }
