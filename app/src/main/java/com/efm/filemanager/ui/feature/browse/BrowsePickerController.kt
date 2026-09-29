package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.domain.model.FileEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the move/copy destination-picker's state and navigation -- split out of BrowseViewModel
 * (a cohesive sub-feature, not a threshold dodge) once adding view-mode support there pushed
 * that class past detekt's function-count ceiling.
 */
class BrowsePickerController(
    private val scope: CoroutineScope,
    private val repository: DocumentTreeRepository,
    private val fileOperationsRepository: FileOperationsRepository,
    private val currentSourceParentUri: () -> Uri?,
    private val currentRootCrumb: () -> BreadcrumbEntry?,
    private val onOperationFailed: suspend () -> Unit,
) {
    private val _pickerState = MutableStateFlow(PickerUiState())
    val pickerState: StateFlow<PickerUiState> = _pickerState.asStateFlow()

    private var observeJob: Job? = null
    private var pendingEntries: List<FileEntry> = emptyList()

    fun openMovePicker(entries: List<FileEntry>) = open(PickerPurpose.MOVE, entries)

    fun openCopyPicker(entries: List<FileEntry>) = open(PickerPurpose.COPY, entries)

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
        pendingEntries = emptyList()
        _pickerState.value = PickerUiState()
    }

    fun confirm() {
        val targetUri = _pickerState.value.breadcrumbs.lastOrNull()?.uri
        val purpose = _pickerState.value.purpose
        val sourceParentUri = currentSourceParentUri()
        if (targetUri == null || purpose == null || sourceParentUri == null) return
        val entries = pendingEntries
        dismiss()
        scope.launch {
            val failures =
                entries.count { entry ->
                    val result =
                        when (purpose) {
                            PickerPurpose.MOVE -> fileOperationsRepository.move(entry, sourceParentUri, targetUri)
                            PickerPurpose.COPY -> fileOperationsRepository.copy(entry, targetUri)
                        }
                    result.isFailure
                }
            if (failures > 0) onOperationFailed()
        }
    }

    private fun open(
        purpose: PickerPurpose,
        entries: List<FileEntry>,
    ) {
        val startCrumb = currentRootCrumb() ?: return
        pendingEntries = entries
        _pickerState.value = PickerUiState(visible = true, purpose = purpose, breadcrumbs = listOf(startCrumb))
        observeFolder(startCrumb.uri)
    }

    private fun observeFolder(uri: Uri) {
        observeJob?.cancel()
        _pickerState.update { it.copy(isLoading = true) }
        observeJob =
            scope.launch {
                launch { repository.refresh(uri) }
                repository.observeChildren(uri).collect { files ->
                    _pickerState.update { it.copy(folders = files.filter { entry -> entry.isDirectory }, isLoading = false) }
                }
            }
    }
}
