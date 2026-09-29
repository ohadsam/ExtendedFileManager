package com.efm.filemanager.ui.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.metadata.FavoriteRepository
import com.efm.filemanager.data.metadata.FileMetadataRepository
import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.domain.model.FavoriteCollection
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

data class FavoritesUiState(
    val collections: List<FavoriteCollection> = emptyList(),
    val entries: List<FileEntry> = emptyList(),
)

@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        private val favoriteRepository: FavoriteRepository,
        private val fileMetadataRepository: FileMetadataRepository,
        private val previewSessionHolder: PreviewSessionHolder,
    ) : ViewModel() {
        val uiState: StateFlow<FavoritesUiState> =
            combine(favoriteRepository.collections, favoriteRepository.favoritesByFileUri) { collections, _ -> collections }
                .map { collections ->
                    FavoritesUiState(collections = collections, entries = fileMetadataRepository.resolveFavoriteEntries())
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FavoritesUiState())

        fun createCollection(
            name: String,
            parentId: Long?,
        ) {
            viewModelScope.launch { favoriteRepository.createCollection(name, parentId) }
        }

        fun renameCollection(
            collection: FavoriteCollection,
            name: String,
        ) {
            viewModelScope.launch { favoriteRepository.renameCollection(collection, name) }
        }

        fun deleteCollection(collection: FavoriteCollection) {
            viewModelScope.launch { favoriteRepository.deleteCollection(collection) }
        }

        fun moveCollection(
            collection: FavoriteCollection,
            direction: MoveDirection,
        ) {
            viewModelScope.launch { favoriteRepository.moveCollection(collection, direction) }
        }

        fun removeFavorite(entry: FileEntry) {
            viewModelScope.launch { favoriteRepository.removeFavorite(entry.uri) }
        }

        /** Starts a preview session over this collection's own favorited files, starting at [tapped]. */
        fun openPreview(
            collectionEntries: List<FileEntry>,
            tapped: FileEntry,
        ) {
            val session = buildPreviewSession(collectionEntries, tapped) ?: return
            previewSessionHolder.start(session.entries, session.startIndex)
        }
    }
