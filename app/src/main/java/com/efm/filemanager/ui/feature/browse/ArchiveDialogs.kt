package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.components.ArchiveProgressDialog
import com.efm.filemanager.ui.components.ConflictPolicyDialog
import com.efm.filemanager.ui.components.TextInputDialog

private const val DEFAULT_ARCHIVE_NAME = "Archive.zip"

@Composable
internal fun CompressDialog(
    onDismiss: () -> Unit,
    archiveViewModel: ArchiveViewModel,
    selectedEntries: List<FileEntry>,
    parentUri: Uri,
) {
    TextInputDialog(
        title = stringResource(R.string.compress_title),
        confirmLabel = stringResource(R.string.compress_confirm),
        initialValue = DEFAULT_ARCHIVE_NAME,
        onConfirm = { name ->
            archiveViewModel.compress(selectedEntries, parentUri, ensureZipExtension(name))
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ExtractConflictDialog(
    request: ExtractRequest,
    parentUri: Uri,
    archiveViewModel: ArchiveViewModel,
    onDismiss: () -> Unit,
) {
    ConflictPolicyDialog(
        showReplaceWarning = request.replaceOriginal,
        onConfirm = { policy ->
            archiveViewModel.extract(request.archive, parentUri, policy, request.replaceOriginal)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ArchiveProgressOverlay(archiveViewModel: ArchiveViewModel) {
    val state by archiveViewModel.uiState.collectAsStateWithLifecycle()
    if (state.isRunning) {
        ArchiveProgressDialog(
            isCompressing = state.isCompressing,
            processedCount = state.processedCount,
            onCancel = archiveViewModel::cancel,
        )
    }
}

@Composable
internal fun ArchiveDialogsSection(
    request: ArchiveDialogRequest?,
    parentUri: Uri?,
    archiveViewModel: ArchiveViewModel,
    selectedEntries: List<FileEntry>,
    onFinished: () -> Unit,
) {
    if (parentUri != null) {
        when (request) {
            ArchiveDialogRequest.Compress ->
                CompressDialog(
                    onDismiss = onFinished,
                    archiveViewModel = archiveViewModel,
                    selectedEntries = selectedEntries,
                    parentUri = parentUri,
                )
            is ArchiveDialogRequest.Extract ->
                ExtractConflictDialog(
                    request = request.request,
                    parentUri = parentUri,
                    archiveViewModel = archiveViewModel,
                    onDismiss = onFinished,
                )
            null -> Unit
        }
    }
    ArchiveProgressOverlay(archiveViewModel = archiveViewModel)
}

internal fun buildArchiveBarActions(
    selectedEntries: List<FileEntry>,
    onRequest: (ArchiveDialogRequest) -> Unit,
): ArchiveBarActions =
    buildArchiveActions(
        onCompressRequested = { onRequest(ArchiveDialogRequest.Compress) },
        onExtractRequested = { replace ->
            selectedEntries.firstOrNull()?.let { archive ->
                onRequest(ArchiveDialogRequest.Extract(ExtractRequest(archive, replace)))
            }
        },
    )

internal fun buildSelectionBarState(
    selectedUris: List<Uri>,
    selectedEntries: List<FileEntry>,
    selectionActions: SelectionBarActions,
    archiveActions: ArchiveBarActions,
): SelectionBarState =
    SelectionBarState(
        selectedCount = selectedUris.size,
        canExtract = selectedEntries.singleOrNull()?.name?.endsWith(".zip", ignoreCase = true) == true,
        actions = selectionActions,
        archiveActions = archiveActions,
    )

internal fun ensureZipExtension(name: String): String = if (name.endsWith(".zip", ignoreCase = true)) name else "$name.zip"
