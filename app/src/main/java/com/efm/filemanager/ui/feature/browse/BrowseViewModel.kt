package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.cloud.uploadsToDisplay
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.data.documenttree.LockedFileException
import com.efm.filemanager.data.local.CloudUploadEntity
import com.efm.filemanager.data.metadata.ScreenMetadataSupport
import com.efm.filemanager.data.metadata.SelectionMetadataActions
import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.ViewMode
import com.efm.filemanager.ui.feature.preview.buildPreviewSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

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
    UPLOAD,
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

    data class DeleteBlockedByLock(val lockedEntries: List<FileEntry>) : BrowseEvent

    data class AddToVaultBlockedByLock(val lockedEntries: List<FileEntry>) : BrowseEvent

    /** [cause] is the first non-lock failure's exception, when one is available, so the UI can show a specific message. */
    data class OperationFailed(val cause: Throwable? = null) : BrowseEvent
}

@HiltViewModel
class BrowseViewModel
    @Inject
    constructor(
        private val documentTreeAccessManager: DocumentTreeAccessManager,
        private val repository: DocumentTreeRepository,
        private val fileOperationsRepository: FileOperationsRepository,
        private val delegateSupport: BrowseDelegateSupport,
        private val preferencesRepository: PreferencesRepository,
        private val screenMetadataSupport: ScreenMetadataSupport,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BrowseUiState())
        val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<BrowseEvent>()
        val events: SharedFlow<BrowseEvent> = _events.asSharedFlow()

        val metadataActions: SelectionMetadataActions get() = screenMetadataSupport.metadataActions

        val tags: StateFlow<List<FileTag>> =
            screenMetadataSupport.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val favoriteCollections: StateFlow<List<FavoriteCollection>> =
            screenMetadataSupport.favoriteCollections.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                emptyList(),
            )

        val picker =
            BrowsePickerController(
                scope = viewModelScope,
                repository = repository,
                fileOperationsRepository = fileOperationsRepository,
                cloudUploadRepository = delegateSupport.cloudUploadRepository,
                callbacks =
                    BrowsePickerCallbacks(
                        currentSourceParentUri = ::currentParentUri,
                        currentRootCrumb = { _uiState.value.breadcrumbs.firstOrNull() },
                        onOperationFailed = { _events.emit(BrowseEvent.OperationFailed()) },
                    ),
            )

        val activeUploads: StateFlow<List<CloudUploadEntity>> =
            delegateSupport.cloudUploadRepository
                .observeAll()
                .map { uploadsToDisplay(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

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
            delegateSupport.previewSessionHolder.start(session.entries, session.startIndex)
        }

        /** Jumps straight to a location found via search, rebuilding its breadcrumb trail. */
        fun navigateToLocation(entry: FileEntry) {
            viewModelScope.launch {
                val targetFolderUri = if (entry.isDirectory) entry.uri else (repository.parentUriOf(entry.uri) ?: return@launch)
                jumpToFolder(targetFolderUri)
            }
        }

        /** The directory-only half of [navigateToLocation], reusable by anything that only ever has a folder's own URI. */
        fun navigateToFolder(folderUri: Uri) {
            viewModelScope.launch { jumpToFolder(folderUri) }
        }

        private suspend fun jumpToFolder(folderUri: Uri) {
            val chain = repository.ancestorChain(folderUri)
            val rootCrumb = BreadcrumbEntry(chain.rootUri, documentTreeAccessManager.rootLabel(chain.rootUri))
            val folderCrumbs = chain.folders.map { folder -> BreadcrumbEntry(folder.uri, folder.name) }
            _uiState.update { it.copy(breadcrumbs = listOf(rootCrumb) + folderCrumbs) }
            observeFolder(folderUri)
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
                val results = entries.associateWith { entry -> fileOperationsRepository.delete(entry, parentUri) }
                val trashedIds = results.values.mapNotNull { it.getOrNull() }
                val lockedEntries = results.filterValues { it.exceptionOrNull() is LockedFileException }.keys.toList()
                val otherFailureCount = entries.size - trashedIds.size - lockedEntries.size
                if (trashedIds.isNotEmpty()) _events.emit(BrowseEvent.UndoDelete(trashedIds))
                if (lockedEntries.isNotEmpty()) _events.emit(BrowseEvent.DeleteBlockedByLock(lockedEntries))
                if (otherFailureCount > 0) _events.emit(BrowseEvent.OperationFailed(firstOtherFailureCause(results.values)))
            }
        }

        fun addToVault(entries: List<FileEntry>) {
            viewModelScope.launch {
                val results = entries.associateWith { entry -> delegateSupport.vaultRepository.addToVault(entry) }
                val lockedEntries = results.filterValues { it.exceptionOrNull() is LockedFileException }.keys.toList()
                val otherFailureCount = entries.size - results.count { it.value.isSuccess } - lockedEntries.size
                if (lockedEntries.isNotEmpty()) _events.emit(BrowseEvent.AddToVaultBlockedByLock(lockedEntries))
                if (otherFailureCount > 0) _events.emit(BrowseEvent.OperationFailed(firstOtherFailureCause(results.values)))
            }
        }

        fun undoDelete(trashedFileIds: List<Long>) {
            viewModelScope.launch {
                trashedFileIds.forEach { id -> fileOperationsRepository.restore(id) }
            }
        }

        fun retryUpload(id: Long) {
            viewModelScope.launch { delegateSupport.cloudUploadRepository.retry(id) }
        }

        fun dismissUpload(id: Long) {
            viewModelScope.launch { delegateSupport.cloudUploadRepository.dismiss(id) }
        }

        private fun runAgainstCurrentFolder(operation: suspend (Uri) -> Result<Unit>) {
            val parentUri = currentParentUri() ?: return
            viewModelScope.launch {
                val result = operation(parentUri)
                if (result.isFailure) _events.emit(BrowseEvent.OperationFailed(result.exceptionOrNull()))
            }
        }

        /** The first failure that isn't [LockedFileException] -- that case already gets its own, more specific event. */
        private fun firstOtherFailureCause(results: Collection<Result<*>>): Throwable? =
            results.firstOrNull { it.isFailure && it.exceptionOrNull() !is LockedFileException }?.exceptionOrNull()

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
