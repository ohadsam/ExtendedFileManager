package com.efm.filemanager.ui.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.metadata.TagRepository
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.TagColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageTagsUiState(val tags: List<FileTag> = emptyList())

@HiltViewModel
class ManageTagsViewModel
    @Inject
    constructor(
        private val tagRepository: TagRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ManageTagsUiState> =
            tagRepository.tags
                .map { tags -> ManageTagsUiState(tags) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ManageTagsUiState())

        fun createTag(
            name: String,
            color: TagColor,
        ) {
            viewModelScope.launch { tagRepository.createTag(name, color) }
        }

        fun updateTag(
            tag: FileTag,
            name: String,
            color: TagColor,
        ) {
            viewModelScope.launch { tagRepository.updateTag(tag, name, color) }
        }

        fun togglePinned(tag: FileTag) {
            viewModelScope.launch { tagRepository.setPinned(tag, !tag.pinned) }
        }

        fun deleteTag(tag: FileTag) {
            viewModelScope.launch { tagRepository.deleteTag(tag) }
        }

        fun mergeTags(
            from: FileTag,
            into: FileTag,
        ) {
            viewModelScope.launch { tagRepository.mergeTags(from, into) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
