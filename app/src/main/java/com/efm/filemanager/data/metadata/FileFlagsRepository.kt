package com.efm.filemanager.data.metadata

import android.net.Uri
import com.efm.filemanager.data.local.FileFlagsDao
import com.efm.filemanager.data.local.FileFlagsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Per-file lock-against-deletion and free-text note -- the two simplest of Phase 9's four dimensions. */
class FileFlagsRepository
    @Inject
    constructor(
        private val fileFlagsDao: FileFlagsDao,
    ) {
        val flagsByFileUri: Flow<Map<String, FileFlagsEntity>> =
            fileFlagsDao.observeAll().map { flags -> flags.associateBy { it.fileUri } }

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
    }
