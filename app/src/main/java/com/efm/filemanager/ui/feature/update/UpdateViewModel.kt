package com.efm.filemanager.ui.feature.update

import android.app.DownloadManager
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.update.UpdateCheckRepository
import com.efm.filemanager.data.update.UpdateCheckResult
import com.efm.filemanager.data.update.UpdateDownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 1_000L

/**
 * Settings' "Check for updates" button is the only thing that ever calls [checkForUpdate] --
 * there's no background/automatic check anywhere in this app (see [UpdateCheckRepository]'s own
 * doc for why). Download progress is left to the system [DownloadManager] notification; this
 * polls just for the terminal state (done/failed), not byte-level progress, since that's all the
 * dialog needs to decide what to show next.
 */
@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        private val updateCheckRepository: UpdateCheckRepository,
        private val updateDownloadRepository: UpdateDownloadRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Hidden)
        val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

        private var pendingUpdate: UpdateCheckResult.UpdateAvailable? = null
        private var downloadId: Long? = null

        fun checkForUpdate() {
            _state.value = UpdateCheckState.Checking
            viewModelScope.launch {
                when (val result = updateCheckRepository.checkForUpdate()) {
                    UpdateCheckResult.UpToDate -> _state.value = UpdateCheckState.UpToDate
                    is UpdateCheckResult.UpdateAvailable -> {
                        pendingUpdate = result
                        _state.value = UpdateCheckState.Available(result.versionName, result.releaseNotes)
                    }
                    is UpdateCheckResult.Error -> {
                        Timber.e("Update check failed: %s", result.message)
                        _state.value = UpdateCheckState.Failed(UpdateFailureReason.NETWORK_ERROR)
                    }
                }
            }
        }

        /** Called once the caller has confirmed (or already holds) the install-unknown-apps permission. */
        fun startDownload() {
            val update = pendingUpdate
            val url = update?.apkDownloadUrl
            if (update == null || url == null) {
                _state.value = UpdateCheckState.Failed(UpdateFailureReason.NO_APK_ATTACHED)
                return
            }
            _state.value = UpdateCheckState.Downloading
            downloadId = updateDownloadRepository.enqueueDownload(url, update.apkFileName)
            pollDownload()
        }

        fun needsInstallPermission() {
            _state.value = UpdateCheckState.NeedsInstallPermission
        }

        fun installIntent(): Intent? {
            val fileName = pendingUpdate?.apkFileName ?: return null
            return updateDownloadRepository.verifiedInstallIntent(fileName)
        }

        fun dismiss() {
            _state.value = UpdateCheckState.Hidden
        }

        private fun pollDownload() {
            val id = downloadId ?: return
            viewModelScope.launch {
                while (isActive) {
                    when (updateDownloadRepository.downloadStatus(id)) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            _state.value = resolveDownloadedState()
                            return@launch
                        }
                        DownloadManager.STATUS_FAILED -> {
                            _state.value = UpdateCheckState.Failed(UpdateFailureReason.DOWNLOAD_FAILED)
                            return@launch
                        }
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }

        private fun resolveDownloadedState(): UpdateCheckState =
            if (installIntent() != null) {
                UpdateCheckState.ReadyToInstall
            } else {
                Timber.e("Downloaded update APK signature didn't match the installed app -- refusing to install")
                UpdateCheckState.Failed(UpdateFailureReason.SIGNATURE_MISMATCH)
            }
    }
