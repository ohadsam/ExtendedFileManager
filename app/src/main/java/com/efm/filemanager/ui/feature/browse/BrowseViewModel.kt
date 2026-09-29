package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.ViewMode
import com.efm.filemanager.ui.feature.preview.PreviewSessionHolder
import com.efm.filemanager.ui.feature.preview.buildPreviewSession
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

data class BreadcrumbEntry(
    val uri: Uri,
    val label: String,
)

data class BrowseUiState(
    val hasAccess: Boolean = false,
    val breadcrumbs: List<BreadcrumbEntry> = emptyList(),
    val files: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
    val querySpec: QuerySpec = QuerySpec(),
    val viewMode: ViewMode = ViewMode.LIST,
)

enum class PickerPurpose {
    MOVE,
    COPY,
}

data class PickerUiState(
    val visible: Boolean = false,
    val purpose: PickerPurpose? = null,
    val breadcrumbs: List<BreadcrumbEntry> = emptyList(),
    val folders: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface BrowseEvent {
    data class UndoDelete(val trashedFileIds: List<Long>) : BrowseEvent

    data object OperationFailed : BrowseEvent
}

@HiltViewModel
class BrowseViewModel
    @Inject
    constructor(
        private val documentTreeAccessManager: DocumentTreeAccessManager,
        private val repository: DocumentTreeRepository,
        private val fileOperationsRepository: FileOperationsRepository,
        private val previewSessionHolder: PreviewSessionHolder,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BrowseUiState())
        val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<BrowseEvent>()
        val events: SharedFlow<BrowseEvent> = _events.asSharedFlow()

        val picker =
            BrowsePickerController(
                scope = viewModelScope,
                repository = repository,
                fileOperationsRepository = fileOperationsRepository,
                currentSourceParentUri = ::currentParentUri,
                currentRootCrumb = { _uiState.value.breadcrumbs.firstOrNull() },
                onOperationFailed = { _events.emit(BrowseEvent.OperationFailed) },
            )

        private var observeJob: Job? = null

        init {
            refreshAccessState()
            viewModelScope.launch {
                preferencesRepository.viewMode.collect { mode -> _uiState.update { it.copy(viewMode = mode) } }
            }
        }

        fun setViewMode(mode: ViewMode) {
            viewModelScope.launch { preferencesRepository.setViewMode(mode) }
        }

        fun refreshAccessState() {
            val granted = documentTreeAccessManager.grantedTreeUris()
            if (granted.isEmpty()) {
                _uiState.update { it.copy(hasAccess = false, breadcrumbs = emptyList(), files = emptyList()) }
                return
            }
            _uiState.update { it.copy(hasAccess = true) }
            if (_uiState.value.breadcrumbs.isEmpty()) {
                val root = granted.first()
                navigateTo(BreadcrumbEntry(root, documentTreeAccessManager.rootLabel(root)))
            }
        }

        fun onTreeGranted(uri: Uri) {
            documentTreeAccessManager.persistAccess(uri)
            _uiState.update { it.copy(breadcrumbs = emptyList()) }
            refreshAccessState()
        }

        fun openEntry(entry: FileEntry) {
            if (!entry.isDirectory) return
            navigateTo(BreadcrumbEntry(entry.uri, entry.name))
        }

        fun updateQuerySpec(spec: QuerySpec) {
            _uiState.update { it.copy(querySpec = spec) }
        }

        /** Starts a preview session over the current folder's previewable files, starting at [entry]. */
        fun openPreview(entry: FileEntry) {
            val session = buildPreviewSession(_uiState.value.files, entry) ?: return
            previewSessionHolder.start(session.entries, session.startIndex)
        }

        /** Jumps straight to a location found via search, rebuilding its breadcrumb trail. */
        fun navigateToLocation(entry: FileEntry) {
            viewModelScope.launch {
                val targetFolderUri = if (entry.isDirectory) entry.uri else (repository.parentUriOf(entry.uri) ?: return@launch)
                val chain = repository.ancestorChain(targetFolderUri)
                val rootCrumb = BreadcrumbEntry(chain.rootUri, documentTreeAccessManager.rootLabel(chain.rootUri))
                val folderCrumbs = chain.folders.map { folder -> BreadcrumbEntry(folder.uri, folder.name) }
                _uiState.update { it.copy(breadcrumbs = listOf(rootCrumb) + folderCrumbs) }
                observeFolder(targetFolderUri)
            }
        }

        fun navigateToBreadcrumb(index: Int) {
            val breadcrumbs = _uiState.value.breadcrumbs
            val target = breadcrumbs.getOrNull(index) ?: return
            _uiState.update { it.copy(breadcrumbs = breadcrumbs.take(index + 1)) }
            observeFolder(target.uri)
        }

        fun createFolder(name: String) = runAgainstCurrentFolder { parentUri -> fileOperationsRepository.createFolder(parentUri, name) }

        fun createFile(name: String) = runAgainstCurrentFolder { parentUri -> fileOperationsRepository.createFile(parentUri, name) }

        fun rename(
            entry: FileEntry,
            newName: String,
        ) = runAgainstCurrentFolder { parentUri -> fileOperationsRepository.rename(entry, parentUri, newName) }

        fun deleteEntries(entries: List<FileEntry>) {
            val parentUri = currentParentUri() ?: return
            viewModelScope.launch {
                val trashedIds = entries.mapNotNull { entry -> fileOperationsRepository.delete(entry, parentUri).getOrNull() }
                if (trashedIds.isNotEmpty()) _events.emit(BrowseEvent.UndoDelete(trashedIds))
                if (trashedIds.size < entries.size) _events.emit(BrowseEvent.OperationFailed)
            }
        }

        fun undoDelete(trashedFileIds: List<Long>) {
            viewModelScope.launch {
                trashedFileIds.forEach { id -> fileOperationsRepository.restore(id) }
            }
        }

        private fun runAgainstCurrentFolder(operation: suspend (Uri) -> Result<Unit>) {
            val parentUri = currentParentUri() ?: return
            viewModelScope.launch {
                if (operation(parentUri).isFailure) _events.emit(BrowseEvent.OperationFailed)
            }
        }

        private fun currentParentUri(): Uri? = _uiState.value.breadcrumbs.lastOrNull()?.uri

        private fun navigateTo(entry: BreadcrumbEntry) {
            _uiState.update { it.copy(breadcrumbs = it.breadcrumbs + entry) }
            observeFolder(entry.uri)
        }

        private fun observeFolder(uri: Uri) {
            observeJob?.cancel()
            _uiState.update { it.copy(isLoading = true) }
            observeJob =
                viewModelScope.launch {
                    launch { repository.refresh(uri) }
                    repository.observeChildren(uri).collect { files ->
                        _uiState.update { it.copy(files = files, isLoading = false) }
                    }
                }
        }
    }
