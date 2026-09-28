package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.domain.model.FileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BreadcrumbEntry(
    val uri: Uri,
    val label: String,
)

data class BrowseUiState(
    val hasAccess: Boolean = false,
    val breadcrumbs: List<BreadcrumbEntry> = emptyList(),
    val files: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val documentTreeAccessManager: DocumentTreeAccessManager,
    private val repository: DocumentTreeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

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

    fun navigateToBreadcrumb(index: Int) {
        val breadcrumbs = _uiState.value.breadcrumbs
        val target = breadcrumbs.getOrNull(index) ?: return
        _uiState.update { it.copy(breadcrumbs = breadcrumbs.take(index + 1)) }
        observeFolder(target.uri)
    }

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
