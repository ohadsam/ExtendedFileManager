package com.efm.filemanager.ui.feature.browse

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog
import com.efm.filemanager.ui.components.CreateEntryDialog
import com.efm.filemanager.ui.components.NewEntryType
import com.efm.filemanager.ui.components.TextInputDialog

internal enum class BrowseDialog {
    CREATE,
    RENAME,
    DELETE,
}

@Composable
internal fun BrowseSnackbarEffect(
    viewModel: BrowseViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val undoDeleteMessage = stringResource(R.string.delete_undo_message)
    val undoLabel = stringResource(R.string.undo)
    val operationFailedMessage = stringResource(R.string.operation_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseEvent.UndoDelete -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = undoDeleteMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short,
                        )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.trashedFileIds)
                }
                BrowseEvent.OperationFailed -> snackbarHostState.showSnackbar(operationFailedMessage)
            }
        }
    }
}

@Composable
internal fun BrowseDialogs(
    dialog: BrowseDialog?,
    onDismissDialog: () -> Unit,
    viewModel: BrowseViewModel,
    selectedEntries: List<FileEntry>,
    pickerState: PickerUiState,
    onClearSelection: () -> Unit,
) {
    when (dialog) {
        BrowseDialog.CREATE ->
            CreateEntryDialog(
                onConfirm = { name, type ->
                    createEntry(viewModel, name, type)
                    onDismissDialog()
                },
                onDismiss = onDismissDialog,
            )
        BrowseDialog.RENAME ->
            RenameDialog(
                target = selectedEntries.firstOrNull(),
                viewModel = viewModel,
                onClearSelection = onClearSelection,
                onDismissDialog = onDismissDialog,
            )
        BrowseDialog.DELETE ->
            ConfirmDangerousActionDialog(
                title = stringResource(R.string.delete_confirm_title),
                message = stringResource(R.string.delete_confirm_message, selectedEntries.size),
                confirmLabel = stringResource(R.string.delete_confirm_button),
                onConfirm = {
                    viewModel.deleteEntries(selectedEntries)
                    onClearSelection()
                    onDismissDialog()
                },
                onDismiss = onDismissDialog,
            )
        null -> Unit
    }

    if (pickerState.visible) {
        DestinationPickerDialog(
            state = pickerState,
            onNavigateInto = viewModel::pickerNavigateInto,
            onNavigateToBreadcrumb = viewModel::pickerNavigateToBreadcrumb,
            onConfirm = viewModel::confirmPicker,
            onDismiss = viewModel::dismissPicker,
        )
    }
}

@Composable
private fun RenameDialog(
    target: FileEntry?,
    viewModel: BrowseViewModel,
    onClearSelection: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    if (target == null) return
    TextInputDialog(
        title = stringResource(R.string.rename_title),
        confirmLabel = stringResource(R.string.rename_confirm),
        initialValue = target.name,
        onConfirm = { newName ->
            viewModel.rename(target, newName)
            onClearSelection()
            onDismissDialog()
        },
        onDismiss = onDismissDialog,
    )
}

private fun createEntry(
    viewModel: BrowseViewModel,
    name: String,
    type: NewEntryType,
) {
    when (type) {
        NewEntryType.FOLDER -> viewModel.createFolder(name)
        NewEntryType.FILE -> viewModel.createFile(name)
    }
}
