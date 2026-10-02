package com.efm.filemanager.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.documenttree.LockedFileException
import com.efm.filemanager.data.metadata.ScreenMetadataSupport
import com.efm.filemanager.data.metadata.SelectionMetadataActions
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.data.search.SearchIndexRepository
import com.efm.filemanager.data.vault.VaultRepository
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.ui.feature.preview.PreviewSessionHolder
import com.efm.filemanager.ui.feature.preview.buildPreviewSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 300L
private const val STOP_TIMEOUT_MS = 5_000L

sealed interface SearchEvent {
    data class AddToVaultBlockedByLock(val lockedEntries: List<FileEntry>) : SearchEvent

    data object OperationFailed : SearchEvent
}

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchIndexRepository: SearchIndexRepository,
        private val vaultRepository: VaultRepository,
        private val previewSessionHolder: PreviewSessionHolder,
        private val preferencesRepository: PreferencesRepository,
        private val screenMetadataSupport: ScreenMetadataSupport,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SearchUiState())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<SearchEvent>()
        val events: SharedFlow<SearchEvent> = _events.asSharedFlow()

        val metadataActions: SelectionMetadataActions get() = screenMetadataSupport.metadataActions

        val tags: StateFlow<List<FileTag>> =
            screenMetadataSupport.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val favoriteCollections: StateFlow<List<FavoriteCollection>> =
            screenMetadataSupport.favoriteCollections.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                emptyList(),
            )

        private var searchJob: Job? = null

        init {
            rebuildIndex()
            viewModelScope.launch {
                preferencesRepository.viewMode.collect { mode -> _uiState.update { it.copy(viewMode = mode) } }
            }
        }

        fun rebuildIndex() {
            viewModelScope.launch {
                _uiState.update { it.copy(isIndexing = true) }
                searchIndexRepository.rebuildIndex()
                _uiState.update { it.copy(isIndexing = false) }
                runSearchNow(_uiState.value.querySpec.freeText)
            }
        }

        /** Starts a preview session over the current results, starting at [entry]. */
        fun openPreview(entry: FileEntry) {
            val session = buildPreviewSession(_uiState.value.results, entry) ?: return
            previewSessionHolder.start(session.entries, session.startIndex)
        }

        fun addToVault(entries: List<FileEntry>) {
            viewModelScope.launch {
                val results = entries.associateWith { entry -> vaultRepository.addToVault(entry) }
                val lockedEntries = results.filterValues { it.exceptionOrNull() is LockedFileException }.keys.toList()
                val otherFailureCount = entries.size - results.count { it.value.isSuccess } - lockedEntries.size
                if (lockedEntries.isNotEmpty()) _events.emit(SearchEvent.AddToVaultBlockedByLock(lockedEntries))
                if (otherFailureCount > 0) _events.emit(SearchEvent.OperationFailed)
            }
        }

        fun updateQuerySpec(spec: QuerySpec) {
            val textChanged = spec.freeText != _uiState.value.querySpec.freeText
            _uiState.update { it.copy(querySpec = spec) }
            if (textChanged) debounceSearch(spec.freeText)
        }

        private fun debounceSearch(text: String) {
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    runSearchNow(text)
                }
        }

        private suspend fun runSearchNow(text: String) {
            val results = if (text.isBlank()) emptyList() else searchIndexRepository.search(text)
            _uiState.update { it.copy(results = results, hasSearched = text.isNotBlank()) }
        }
    }
