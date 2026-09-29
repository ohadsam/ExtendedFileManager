package com.efm.filemanager.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.metadata.SelectionMetadataActions
import com.efm.filemanager.data.metadata.TagRepository
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.data.search.SearchIndexRepository
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.ui.feature.preview.PreviewSessionHolder
import com.efm.filemanager.ui.feature.preview.buildPreviewSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 300L
private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchIndexRepository: SearchIndexRepository,
        private val previewSessionHolder: PreviewSessionHolder,
        private val preferencesRepository: PreferencesRepository,
        private val tagRepository: TagRepository,
        val metadataActions: SelectionMetadataActions,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SearchUiState())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        val tags: StateFlow<List<FileTag>> =
            tagRepository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

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
