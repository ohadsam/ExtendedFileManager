package com.efm.filemanager.ui.feature.browse

import com.efm.filemanager.R
import com.efm.filemanager.data.cloud.CloudUploadStatus

internal fun CloudUploadStatus.labelRes(): Int =
    when (this) {
        CloudUploadStatus.QUEUED -> R.string.upload_status_queued
        CloudUploadStatus.UPLOADING -> R.string.upload_status_uploading
        CloudUploadStatus.DONE -> R.string.upload_status_done
        CloudUploadStatus.FAILED -> R.string.upload_status_failed
    }
