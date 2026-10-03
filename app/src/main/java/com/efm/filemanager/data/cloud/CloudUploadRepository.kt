package com.efm.filemanager.data.cloud

import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.efm.filemanager.data.local.CloudUploadDao
import com.efm.filemanager.data.local.CloudUploadEntity
import com.efm.filemanager.domain.model.FileEntry
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Tracks Phase 16's "upload to a cloud-backed folder" transfers. Deliberately not a direct
 * Google Drive/Dropbox SDK integration -- the destination is any SAF folder the user already
 * granted (Drive's own app already registers as a `DocumentsProvider`, so picking a Drive folder
 * through the existing destination picker needs no OAuth or credentials this app would have to
 * hold). Each file becomes one [CloudUploadEntity] row plus one [CloudUploadWorker] job, so a
 * large multi-file upload survives the app leaving the foreground and each file's own
 * success/failure is tracked independently -- one failure never hides the others' progress.
 */
class CloudUploadRepository
    @Inject
    constructor(
        private val cloudUploadDao: CloudUploadDao,
        private val workManager: WorkManager,
    ) {
        fun observeAll(): Flow<List<CloudUploadEntity>> = cloudUploadDao.observeAll()

        suspend fun enqueueUploads(
            entries: List<FileEntry>,
            destinationParentUri: Uri,
        ) {
            Timber.i("CloudUpload: queuing %d file(s) for upload to %s", entries.size, destinationParentUri)
            entries.forEach { entry -> enqueueOne(entry, destinationParentUri) }
        }

        /** Re-queues a previously [CloudUploadStatus.FAILED] entry from scratch, reusing its original source/destination. */
        suspend fun retry(id: Long) {
            val entity = cloudUploadDao.getById(id)
            if (entity == null) {
                Timber.w("CloudUpload: retry requested for unknown upload id=%d", id)
                return
            }
            Timber.i("CloudUpload: retrying upload id=%d name='%s'", id, entity.sourceName)
            val requeued = entity.copy(status = CloudUploadStatus.QUEUED.name, updatedAt = System.currentTimeMillis(), errorMessage = null)
            cloudUploadDao.update(requeued)
            enqueueWork(id)
        }

        /** Clears a finished (done or failed) entry from the visible list -- in-flight entries are left alone. */
        suspend fun dismiss(id: Long) {
            val entity = cloudUploadDao.getById(id)
            if (entity == null) {
                Timber.w("CloudUpload: dismiss requested for unknown upload id=%d", id)
                return
            }
            if (entity.status == CloudUploadStatus.QUEUED.name || entity.status == CloudUploadStatus.UPLOADING.name) {
                Timber.w("CloudUpload: ignoring dismiss for still-active upload id=%d name='%s'", id, entity.sourceName)
                return
            }
            Timber.i("CloudUpload: dismissing %s upload id=%d name='%s'", entity.status, id, entity.sourceName)
            cloudUploadDao.delete(entity)
        }

        private suspend fun enqueueOne(
            entry: FileEntry,
            destinationParentUri: Uri,
        ) {
            val now = System.currentTimeMillis()
            val id =
                cloudUploadDao.insert(
                    CloudUploadEntity(
                        sourceUri = entry.uri.toString(),
                        sourceName = entry.name,
                        destinationParentUri = destinationParentUri.toString(),
                        status = CloudUploadStatus.QUEUED.name,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            Timber.i("CloudUpload: queued id=%d name='%s' source=%s destination=%s", id, entry.name, entry.uri, destinationParentUri)
            enqueueWork(id)
        }

        private fun enqueueWork(id: Long) {
            val inputData = workDataOf(CloudUploadWorker.KEY_UPLOAD_ID to id)
            val request = OneTimeWorkRequestBuilder<CloudUploadWorker>().setInputData(inputData).build()
            workManager.enqueueUniqueWork(cloudUploadWorkName(id), ExistingWorkPolicy.REPLACE, request)
            Timber.i("CloudUpload: enqueued WorkManager job for upload id=%d (work name=%s)", id, cloudUploadWorkName(id))
        }
    }

internal fun cloudUploadWorkName(id: Long): String = "cloud_upload_$id"

/** Done uploads don't need to keep showing once they've succeeded -- only active/failed ones are worth a persistent banner. */
fun uploadsToDisplay(all: List<CloudUploadEntity>): List<CloudUploadEntity> = all.filter { it.status != CloudUploadStatus.DONE.name }
