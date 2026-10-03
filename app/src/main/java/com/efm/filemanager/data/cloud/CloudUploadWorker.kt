package com.efm.filemanager.data.cloud

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.data.local.CloudUploadDao
import com.efm.filemanager.data.local.CloudUploadEntity
import com.efm.filemanager.domain.model.FileEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Performs one queued upload by reusing [FileOperationsRepository.copy] -- the destination is
 * just another SAF folder (which may be Drive-backed), so this is the exact same streaming-copy
 * code path Browse's own Copy action already uses, with its own `PathGuard` checks and audit
 * logging along for free. Heavily logged (every state transition, both directions) since an
 * upload runs in the background with no UI watching it live -- Phase 12's Logs screen is the
 * place to look when a transfer silently didn't finish.
 */
@HiltWorker
class CloudUploadWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val cloudUploadDao: CloudUploadDao,
        private val fileOperationsRepository: FileOperationsRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val id = inputData.getLong(KEY_UPLOAD_ID, -1L)
            val entity = if (id >= 0) cloudUploadDao.getById(id) else null
            if (entity == null) {
                Timber.e("CloudUploadWorker: missing/invalid upload id=%d or no matching row, aborting", id)
                return Result.failure()
            }
            Timber.i("CloudUploadWorker: starting id=%d name='%s' destination=%s", id, entity.sourceName, entity.destinationParentUri)
            return runUpload(entity)
        }

        private suspend fun runUpload(entity: CloudUploadEntity): Result {
            markUploading(entity)
            val destinationUri = Uri.parse(entity.destinationParentUri)
            val sourceEntry = placeholderEntryFor(entity)
            Timber.i("CloudUploadWorker: copying id=%d '%s' -> %s", entity.id, entity.sourceName, destinationUri)
            val result = fileOperationsRepository.copy(sourceEntry, destinationUri)
            return result.fold(
                onSuccess = {
                    Timber.i("CloudUploadWorker: succeeded id=%d name='%s'", entity.id, entity.sourceName)
                    markDone(entity)
                    Result.success()
                },
                onFailure = { error ->
                    Timber.e(error, "CloudUploadWorker: failed id=%d name='%s'", entity.id, entity.sourceName)
                    markFailed(entity, error.message)
                    Result.failure()
                },
            )
        }

        private suspend fun markUploading(entity: CloudUploadEntity) {
            Timber.i("CloudUploadWorker: marking id=%d as UPLOADING", entity.id)
            cloudUploadDao.update(entity.copy(status = CloudUploadStatus.UPLOADING.name, updatedAt = System.currentTimeMillis()))
        }

        private suspend fun markDone(entity: CloudUploadEntity) {
            val updated = entity.copy(status = CloudUploadStatus.DONE.name, updatedAt = System.currentTimeMillis(), errorMessage = null)
            cloudUploadDao.update(updated)
        }

        private suspend fun markFailed(
            entity: CloudUploadEntity,
            message: String?,
        ) {
            val updated =
                entity.copy(status = CloudUploadStatus.FAILED.name, updatedAt = System.currentTimeMillis(), errorMessage = message)
            cloudUploadDao.update(updated)
        }

        private fun placeholderEntryFor(entity: CloudUploadEntity): FileEntry =
            FileEntry(
                uri = Uri.parse(entity.sourceUri),
                documentId = entity.sourceUri,
                name = entity.sourceName,
                isDirectory = false,
                size = 0L,
                lastModified = 0L,
                mimeType = null,
                sourceApp = null,
            )

        companion object {
            const val KEY_UPLOAD_ID = "upload_id"
        }
    }
