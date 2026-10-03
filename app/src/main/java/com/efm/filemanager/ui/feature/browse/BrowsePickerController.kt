package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import com.efm.filemanager.data.cloud.CloudUploadRepository
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
import timber.log.Timber

/**
 * Owns the move/copy/upload destination-picker's state and navigation -- split out of
 * BrowseViewModel (a cohesive sub-feature, not a threshold dodge) once adding view-mode support
 * there pushed that class past detekt's function-count ceiling. [callbacks] bundles what used to
 * be three separate lambda parameters, freeing a constructor slot for [cloudUploadRepository]
 * without tripping `LongParameterList` again.
 */
class BrowsePickerController(
    private val scope: CoroutineScope,
    private val repository: DocumentTreeRepository,
    private val fileOperationsRepository: FileOperationsRepository,
    private val cloudUploadRepository: CloudUploadRepository,
    private val callbacks: BrowsePickerCallbacks,
) {
    private val _pickerState = MutableStateFlow(PickerUiState())
    val pickerState: StateFlow<PickerUiState> = _pickerState.asStateFlow()

    private var observeJob: Job? = null
    private var pendingEntries: List<FileEntry> = emptyList()

    fun openMovePicker(entries: List<FileEntry>) = open(PickerPurpose.MOVE, entries)

    fun openCopyPicker(entries: List<FileEntry>) = open(PickerPurpose.COPY, entries)

    fun openUploadPicker(entries: List<FileEntry>) = open(PickerPurpose.UPLOAD, entries)

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
        if (targetUri == null || purpose == null) return
        val entries = pendingEntries
        dismiss()
        if (purpose == PickerPurpose.UPLOAD) {
            confirmUpload(entries, targetUri)
        } else {
            confirmMoveOrCopy(purpose, entries, targetUri)
        }
    }

    private fun confirmUpload(
        entries: List<FileEntry>,
        targetUri: Uri,
    ) {
        Timber.i("BrowsePickerController: confirming upload of %d file(s) to %s", entries.size, targetUri)
        scope.launch { cloudUploadRepository.enqueueUploads(entries, targetUri) }
    }

    private fun confirmMoveOrCopy(
        purpose: PickerPurpose,
        entries: List<FileEntry>,
        targetUri: Uri,
    ) {
        val sourceParentUri = callbacks.currentSourceParentUri() ?: return
        scope.launch {
            val failures =
                entries.count { entry ->
                    val result =
                        if (purpose == PickerPurpose.MOVE) {
                            fileOperationsRepository.move(entry, sourceParentUri, targetUri)
                        } else {
                            fileOperationsRepository.copy(entry, targetUri)
                        }
                    result.isFailure
                }
            if (failures > 0) callbacks.onOperationFailed()
        }
    }

    private fun open(
        purpose: PickerPurpose,
        entries: List<FileEntry>,
    ) {
        val startCrumb = callbacks.currentRootCrumb() ?: return
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
