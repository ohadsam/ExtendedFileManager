package com.efm.filemanager.ui.feature.update

/** [UpdateViewModel]'s own state machine -- exactly one of these at a time, driving which dialog (if any) [UpdateCheckDialog] shows. */
sealed interface UpdateCheckState {
    data object Hidden : UpdateCheckState

    data object Checking : UpdateCheckState

    data object UpToDate : UpdateCheckState

    data class Available(
        val versionName: String,
        val releaseNotes: String,
    ) : UpdateCheckState

    /** The device hasn't granted this app "install unknown apps" yet -- the download is deferred until that's settled. */
    data object NeedsInstallPermission : UpdateCheckState

    data object Downloading : UpdateCheckState

    data object ReadyToInstall : UpdateCheckState

    data class Failed(val reason: UpdateFailureReason) : UpdateCheckState
}

/** Localized at the Composable layer -- a real exception detail, when there is one, goes to Timber, never into the dialog itself. */
enum class UpdateFailureReason {
    NO_APK_ATTACHED,
    DOWNLOAD_FAILED,
    SIGNATURE_MISMATCH,
    NETWORK_ERROR,
}
