package com.efm.filemanager.ui.feature.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.efm.filemanager.data.advisor.ADVISOR_SCAN_WORK_NAME
import com.efm.filemanager.data.advisor.AdvisorScanProgress
import com.efm.filemanager.data.advisor.StorageAdvisorRepository
import com.efm.filemanager.data.advisor.StorageAdvisorScanWorker
import com.efm.filemanager.data.advisor.toAdvisorScanProgress
import com.efm.filemanager.domain.model.StorageRecommendation
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
class StorageAdvisorViewModel
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val storageAdvisorRepository: StorageAdvisorRepository,
        private val previewSessionHolder: PreviewSessionHolder,
    ) : ViewModel() {
        val uiState: StateFlow<StorageAdvisorUiState> =
            combine(
                workManager.getWorkInfosForUniqueWorkFlow(ADVISOR_SCAN_WORK_NAME).map { infos -> infos.toRunState() },
                storageAdvisorRepository.observeRecommendations(),
            ) { (runState, progress), recommendations -> StorageAdvisorUiState(runState, progress, recommendations) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StorageAdvisorUiState())

        fun startScan() {
            val request = OneTimeWorkRequestBuilder<StorageAdvisorScanWorker>().build()
            workManager.enqueueUniqueWork(ADVISOR_SCAN_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancelScan() {
            workManager.cancelUniqueWork(ADVISOR_SCAN_WORK_NAME)
        }

        /** Dedupes by uri first -- deleting a file removes every category row for it in one go. */
        fun deleteRecommendations(items: List<StorageRecommendation>) {
            viewModelScope.launch {
                items.distinctBy { it.entry.uri }.forEach { storageAdvisorRepository.deleteRecommendation(it) }
            }
        }

        fun dismiss(items: List<StorageRecommendation>) {
            viewModelScope.launch {
                items.forEach { storageAdvisorRepository.dismiss(it) }
            }
        }

        /** Starts a preview session across every previewable recommendation, starting at [tapped]. */
        fun openPreview(
            recommendations: List<StorageRecommendation>,
            tapped: StorageRecommendation,
        ) {
            val session = buildPreviewSession(recommendations.map { it.entry }, tapped.entry) ?: return
            previewSessionHolder.start(session.entries, session.startIndex)
        }
    }

private fun List<WorkInfo>.toRunState(): Pair<AdvisorScanRunState, AdvisorScanProgress?> {
    val info = firstOrNull() ?: return AdvisorScanRunState.IDLE to null
    val state =
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> AdvisorScanRunState.RUNNING
            WorkInfo.State.SUCCEEDED -> AdvisorScanRunState.SUCCEEDED
            WorkInfo.State.FAILED -> AdvisorScanRunState.FAILED
            WorkInfo.State.CANCELLED -> AdvisorScanRunState.CANCELLED
        }
    val progress = if (state == AdvisorScanRunState.RUNNING) info.progress.toAdvisorScanProgress() else null
    return state to progress
}
