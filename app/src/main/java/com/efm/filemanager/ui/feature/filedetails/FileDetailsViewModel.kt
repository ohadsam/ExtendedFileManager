package com.efm.filemanager.ui.feature.filedetails

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.metadata.FavoriteRepository
import com.efm.filemanager.data.metadata.FileFlagsRepository
import com.efm.filemanager.data.metadata.TagRepository
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class FileDetailsViewModel
    @Inject
    constructor(
        private val favoriteRepository: FavoriteRepository,
        private val fileFlagsRepository: FileFlagsRepository,
        private val tagRepository: TagRepository,
    ) : ViewModel() {
        val collections: StateFlow<List<FavoriteCollection>> =
            favoriteRepository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val allTags: StateFlow<List<FileTag>> =
            tagRepository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        fun setFavorite(
            fileUri: Uri,
            collectionId: Long?,
        ) {
            viewModelScope.launch { favoriteRepository.setFavorite(listOf(fileUri), collectionId) }
        }

        fun removeFavorite(fileUri: Uri) {
            viewModelScope.launch { favoriteRepository.removeFavorite(fileUri) }
        }

        fun setLocked(
            fileUri: Uri,
            locked: Boolean,
        ) {
            viewModelScope.launch { fileFlagsRepository.setLocked(listOf(fileUri), locked) }
        }

        fun setNote(
            fileUri: Uri,
            note: String?,
        ) {
            viewModelScope.launch { fileFlagsRepository.setNote(fileUri, note) }
        }

        fun addTag(
            fileUri: Uri,
            tagId: Long,
        ) {
            viewModelScope.launch { tagRepository.addTagToFiles(listOf(fileUri), tagId) }
        }

        fun removeTag(
            fileUri: Uri,
            tagId: Long,
        ) {
            viewModelScope.launch { tagRepository.removeTagFromFile(fileUri, tagId) }
        }
    }
