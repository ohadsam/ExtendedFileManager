package com.efm.filemanager.ui.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.efm.filemanager.data.duplicates.DuplicateScanRepository
import com.efm.filemanager.data.duplicates.DuplicateScanWorker
import com.efm.filemanager.data.duplicates.SCAN_WORK_NAME
import com.efm.filemanager.data.duplicates.ScanProgress
import com.efm.filemanager.data.duplicates.toScanProgress
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.feature.preview.PreviewSessionHolder
import com.efm.filemanager.ui.feature.preview.buildPreviewSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class DuplicatesViewModel
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val duplicateScanRepository: DuplicateScanRepository,
        private val previewSessionHolder: PreviewSessionHolder,
    ) : ViewModel() {
        val uiState: StateFlow<DuplicatesUiState> =
            combine(
                workManager.getWorkInfosForUniqueWorkFlow(SCAN_WORK_NAME).map { infos -> infos.toRunState() },
                duplicateScanRepository.observeGroups(),
            ) { (runState, progress), groups -> DuplicatesUiState(runState = runState, progress = progress, groups = groups) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DuplicatesUiState())

        fun startScan() {
            val request = OneTimeWorkRequestBuilder<DuplicateScanWorker>().build()
            workManager.enqueueUniqueWork(SCAN_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancelScan() {
            workManager.cancelUniqueWork(SCAN_WORK_NAME)
        }

        fun deleteEntries(entries: List<FileEntry>) {
            viewModelScope.launch {
                entries.forEach { entry -> duplicateScanRepository.deleteFile(entry) }
            }
        }

        /** Starts a preview session over one duplicate group's own files, starting at [tapped]. */
        fun openPreview(
            groupFiles: List<FileEntry>,
            tapped: FileEntry,
        ) {
            val session = buildPreviewSession(groupFiles, tapped) ?: return
            previewSessionHolder.start(session.entries, session.startIndex)
        }
    }

private fun List<WorkInfo>.toRunState(): Pair<ScanRunState, ScanProgress?> {
    val info = firstOrNull() ?: return ScanRunState.IDLE to null
    val state =
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> ScanRunState.RUNNING
            WorkInfo.State.SUCCEEDED -> ScanRunState.SUCCEEDED
            WorkInfo.State.FAILED -> ScanRunState.FAILED
            WorkInfo.State.CANCELLED -> ScanRunState.CANCELLED
        }
    val progress = if (state == ScanRunState.RUNNING) info.progress.toScanProgress() else null
    return state to progress
}
