package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.QuerySpec
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BrowseUiState())
        val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

        private val _pickerState = MutableStateFlow(PickerUiState())
        val pickerState: StateFlow<PickerUiState> = _pickerState.asStateFlow()

        private val _events = MutableSharedFlow<BrowseEvent>()
        val events: SharedFlow<BrowseEvent> = _events.asSharedFlow()

        private var observeJob: Job? = null
        private var pickerObserveJob: Job? = null
        private var pendingPickerEntries: List<FileEntry> = emptyList()

        init {
            refreshAccessState()
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

        fun openMovePicker(entries: List<FileEntry>) = openPicker(PickerPurpose.MOVE, entries)

        fun openCopyPicker(entries: List<FileEntry>) = openPicker(PickerPurpose.COPY, entries)

        fun pickerNavigateInto(entry: FileEntry) {
            if (!entry.isDirectory) return
            val updated = _pickerState.value.breadcrumbs + BreadcrumbEntry(entry.uri, entry.name)
            _pickerState.update { it.copy(breadcrumbs = updated) }
            observePickerFolder(entry.uri)
        }

        fun pickerNavigateToBreadcrumb(index: Int) {
            val breadcrumbs = _pickerState.value.breadcrumbs
            val target = breadcrumbs.getOrNull(index) ?: return
            _pickerState.update { it.copy(breadcrumbs = breadcrumbs.take(index + 1)) }
            observePickerFolder(target.uri)
        }

        fun dismissPicker() {
            pickerObserveJob?.cancel()
            pendingPickerEntries = emptyList()
            _pickerState.value = PickerUiState()
        }

        fun confirmPicker() {
            val targetUri = _pickerState.value.breadcrumbs.lastOrNull()?.uri
            val purpose = _pickerState.value.purpose
            val sourceParentUri = currentParentUri()
            if (targetUri == null || purpose == null || sourceParentUri == null) return
            val entries = pendingPickerEntries
            dismissPicker()
            viewModelScope.launch {
                val failures =
                    entries.count { entry ->
                        val result =
                            when (purpose) {
                                PickerPurpose.MOVE -> fileOperationsRepository.move(entry, sourceParentUri, targetUri)
                                PickerPurpose.COPY -> fileOperationsRepository.copy(entry, targetUri)
                            }
                        result.isFailure
                    }
                if (failures > 0) _events.emit(BrowseEvent.OperationFailed)
            }
        }

        private fun openPicker(
            purpose: PickerPurpose,
            entries: List<FileEntry>,
        ) {
            val startCrumb = _uiState.value.breadcrumbs.firstOrNull() ?: return
            pendingPickerEntries = entries
            _pickerState.value = PickerUiState(visible = true, purpose = purpose, breadcrumbs = listOf(startCrumb))
            observePickerFolder(startCrumb.uri)
        }

        private fun observePickerFolder(uri: Uri) {
            pickerObserveJob?.cancel()
            _pickerState.update { it.copy(isLoading = true) }
            pickerObserveJob =
                viewModelScope.launch {
                    launch { repository.refresh(uri) }
                    repository.observeChildren(uri).collect { files ->
                        _pickerState.update { it.copy(folders = files.filter { entry -> entry.isDirectory }, isLoading = false) }
                    }
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
