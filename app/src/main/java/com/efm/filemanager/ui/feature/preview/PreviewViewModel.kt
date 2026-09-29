package com.efm.filemanager.ui.feature.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.metadata.SelectionMetadataActions
import com.efm.filemanager.data.metadata.TagRepository
import com.efm.filemanager.domain.model.FileTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class PreviewViewModel
    @Inject
    constructor(
        private val previewSessionHolder: PreviewSessionHolder,
        private val tagRepository: TagRepository,
        val metadataActions: SelectionMetadataActions,
    ) : ViewModel() {
        val session: StateFlow<PreviewSession?> = previewSessionHolder.session

        val tags: StateFlow<List<FileTag>> =
            tagRepository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        override fun onCleared() {
            previewSessionHolder.clear()
        }
    }
