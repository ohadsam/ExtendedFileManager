package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.archive.ArchiveRepository
import com.efm.filemanager.data.archive.ConflictPolicy
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.domain.model.FileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val isRunning: Boolean = false,
    val processedCount: Int = 0,
    val isCompressing: Boolean = false,
)

sealed interface ArchiveEvent {
    data object Failed : ArchiveEvent
}

@HiltViewModel
class ArchiveViewModel
    @Inject
    constructor(
        private val archiveRepository: ArchiveRepository,
        private val fileOperationsRepository: FileOperationsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ArchiveUiState())
        val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<ArchiveEvent>()
        val events: SharedFlow<ArchiveEvent> = _events.asSharedFlow()

        private var job: Job? = null

        fun compress(
            entries: List<FileEntry>,
            parentUri: Uri,
            archiveName: String,
        ) {
            _uiState.value = ArchiveUiState(isRunning = true, isCompressing = true)
            job =
                viewModelScope.launch {
                    val result =
                        archiveRepository.compress(entries, parentUri, archiveName) { count ->
                            _uiState.update { it.copy(processedCount = count) }
                        }
                    finish(result)
                }
        }

        fun extract(
            archive: FileEntry,
            parentUri: Uri,
            policy: ConflictPolicy,
            replaceOriginal: Boolean,
        ) {
            _uiState.value = ArchiveUiState(isRunning = true, isCompressing = false)
            job =
                viewModelScope.launch {
                    val result =
                        archiveRepository.extract(archive, parentUri, policy) { count ->
                            _uiState.update { it.copy(processedCount = count) }
                        }
                    if (result.isSuccess && replaceOriginal) fileOperationsRepository.delete(archive, parentUri)
                    finish(result)
                }
        }

        fun cancel() {
            job?.cancel()
            _uiState.value = ArchiveUiState()
        }

        private suspend fun finish(result: Result<Unit>) {
            _uiState.value = ArchiveUiState()
            if (result.isFailure) _events.emit(ArchiveEvent.Failed)
        }
    }
