package com.efm.filemanager.ui.feature.update

import com.efm.filemanager.R

internal fun UpdateFailureReason.messageRes(): Int =
    when (this) {
        UpdateFailureReason.NO_APK_ATTACHED -> R.string.update_error_no_apk
        UpdateFailureReason.DOWNLOAD_FAILED -> R.string.update_error_download_failed
        UpdateFailureReason.SIGNATURE_MISMATCH -> R.string.update_error_signature_mismatch
        UpdateFailureReason.NETWORK_ERROR -> R.string.update_error_network
    }
