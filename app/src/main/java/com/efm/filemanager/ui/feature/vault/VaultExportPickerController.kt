package com.efm.filemanager.ui.feature.vault

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.vault.VaultRepository
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.feature.browse.BreadcrumbEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultExportPickerState(
    val visible: Boolean = false,
    val breadcrumbs: List<BreadcrumbEntry> = emptyList(),
    val folders: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * Owns the export-to-folder picker's state and navigation -- a single-destination variant of
 * [com.efm.filemanager.ui.feature.browse.BrowsePickerController] (no move/copy distinction),
 * reusing the same [BreadcrumbEntry] type and the now-generalized
 * [com.efm.filemanager.ui.feature.browse.DestinationPickerDialog] rather than duplicating either.
 */
class VaultExportPickerController(
    private val scope: CoroutineScope,
    private val documentTreeAccessManager: DocumentTreeAccessManager,
    private val documentTreeRepository: DocumentTreeRepository,
    private val vaultRepository: VaultRepository,
    private val onOperationFailed: suspend () -> Unit,
) {
    private val _pickerState = MutableStateFlow(VaultExportPickerState())
    val pickerState: StateFlow<VaultExportPickerState> = _pickerState.asStateFlow()

    private var observeJob: Job? = null
    private var pendingIds: List<Long> = emptyList()

    fun open(ids: List<Long>) {
        val root = documentTreeAccessManager.grantedTreeUris().firstOrNull() ?: return
        pendingIds = ids
        val rootCrumb = BreadcrumbEntry(root, documentTreeAccessManager.rootLabel(root))
        _pickerState.value = VaultExportPickerState(visible = true, breadcrumbs = listOf(rootCrumb))
        observeFolder(root)
    }

    fun navigateInto(entry: FileEntry) {
        if (!entry.isDirectory) return
        val updated = _pickerState.value.breadcrumbs + BreadcrumbEntry(entry.uri, entry.name)
        _pickerState.update { it.copy(breadcrumbs = updated) }
        observeFolder(entry.uri)
    }

    fun navigateToBreadcrumb(index: Int) {
        val breadcrumbs = _pickerState.value.breadcrumbs
        val target = breadcrumbs.getOrNull(index) ?: return
        _pickerState.update { it.copy(breadcrumbs = breadcrumbs.take(index + 1)) }
        observeFolder(target.uri)
    }

    fun dismiss() {
        observeJob?.cancel()
        pendingIds = emptyList()
        _pickerState.value = VaultExportPickerState()
    }

    fun confirm() {
        val targetUri = _pickerState.value.breadcrumbs.lastOrNull()?.uri ?: return
        val ids = pendingIds
        dismiss()
        scope.launch {
            val failures = ids.count { id -> vaultRepository.exportFromVault(id, targetUri).isFailure }
            if (failures > 0) onOperationFailed()
        }
    }

    private fun observeFolder(uri: Uri) {
        observeJob?.cancel()
        _pickerState.update { it.copy(isLoading = true) }
        observeJob =
            scope.launch {
                launch { documentTreeRepository.refresh(uri) }
                documentTreeRepository.observeChildren(uri).collect { files ->
                    _pickerState.update { it.copy(folders = files.filter { entry -> entry.isDirectory }, isLoading = false) }
                }
            }
    }
}
