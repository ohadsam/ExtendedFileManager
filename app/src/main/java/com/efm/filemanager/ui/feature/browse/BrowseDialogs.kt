package com.efm.filemanager.ui.feature.browse

import android.content.Context
import android.content.Intent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.data.security.PathTraversalException
import com.efm.filemanager.data.security.ProtectedPathException
import com.efm.filemanager.data.share.ShareIntentFactory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog
import com.efm.filemanager.ui.components.CreateEntryDialog
import com.efm.filemanager.ui.components.FavoriteCollectionPickerDialog
import com.efm.filemanager.ui.components.NewEntryType
import com.efm.filemanager.ui.components.TagPickerDialog
import com.efm.filemanager.ui.components.TextInputDialog
import com.efm.filemanager.ui.feature.filedetails.FileDetailsSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun BrowseSnackbarEffect(
    viewModel: BrowseViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val undoDeleteMessage = stringResource(R.string.delete_undo_message)
    val undoLabel = stringResource(R.string.undo)
    val unlockLabel = stringResource(R.string.file_details_lock)
    val context = LocalContext.current

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
                is BrowseEvent.DeleteBlockedByLock -> {
                    val message = context.getString(R.string.delete_blocked_by_lock, event.lockedEntries.size)
                    val result = snackbarHostState.showSnackbar(message = message, actionLabel = unlockLabel)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.metadataActions.unlock(event.lockedEntries.map { it.uri })
                    }
                }
                is BrowseEvent.AddToVaultBlockedByLock -> {
                    val message = context.getString(R.string.add_to_vault_blocked_by_lock, event.lockedEntries.size)
                    val result = snackbarHostState.showSnackbar(message = message, actionLabel = unlockLabel)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.metadataActions.unlock(event.lockedEntries.map { it.uri })
                    }
                }
                is BrowseEvent.OperationFailed ->
                    snackbarHostState.showSnackbar(operationFailedMessage(context, event.cause))
            }
        }
    }
}

/**
 * Phase 11's own "unclaimed scope" note closed here: a protected-path/path-traversal denial now
 * gets its own specific wording instead of the generic fallback every other failure still shows.
 * Plain `context.getString` rather than `stringResource`, since this runs inside
 * [LaunchedEffect]'s suspend collector, not composable scope -- same reason [DeleteBlockedByLock]
 * above already resolves its own dynamic-arg message this way.
 */
private fun operationFailedMessage(
    context: Context,
    cause: Throwable?,
): String =
    when (cause) {
        is ProtectedPathException -> context.getString(R.string.error_protected_path)
        is PathTraversalException -> context.getString(R.string.error_path_traversal, cause.attemptedName)
        else -> context.getString(R.string.operation_failed)
    }

/**
 * [onFinished] both dismisses the active dialog and clears the current selection -- every
 * confirm handler below needs both, and Create (which has no selection to clear when it's
 * offered, since the FAB is selection-mode-only) just no-ops on an already-empty clear.
 */
@Composable
internal fun BrowseDialogs(
    dialog: BrowseDialog?,
    onFinished: () -> Unit,
    viewModel: BrowseViewModel,
    context: DialogsContext,
    scope: CoroutineScope,
) {
    when (dialog) {
        BrowseDialog.CREATE ->
            CreateEntryDialog(
                onConfirm = { name, type ->
                    createEntry(viewModel, name, type)
                    onFinished()
                },
                onDismiss = onFinished,
            )
        BrowseDialog.RENAME ->
            RenameDialog(target = context.selectedEntries.firstOrNull(), viewModel = viewModel, onFinished = onFinished)
        BrowseDialog.DELETE ->
            ConfirmDangerousActionDialog(
                title = stringResource(R.string.delete_confirm_title),
                message = stringResource(R.string.delete_confirm_message, context.selectedEntries.size),
                confirmLabel = stringResource(R.string.delete_confirm_button),
                onConfirm = {
                    viewModel.deleteEntries(context.selectedEntries)
                    onFinished()
                },
                onDismiss = onFinished,
            )
        BrowseDialog.TAG_PICKER -> TagPickerCase(context = context, viewModel = viewModel, scope = scope, onFinished = onFinished)
        BrowseDialog.DETAILS ->
            context.selectedEntries.firstOrNull()?.let { entry ->
                FileDetailsSheet(entry = entry, onDismiss = onFinished, onOpenManageTags = context.onOpenManageTags)
            }
        BrowseDialog.FAVORITE_COLLECTION_PICKER ->
            FavoriteCollectionPickerCase(context = context, viewModel = viewModel, scope = scope, onFinished = onFinished)
        BrowseDialog.ADD_TO_VAULT -> AddToVaultCase(context = context, viewModel = viewModel, onFinished = onFinished)
        BrowseDialog.SHARE -> ShareCase(entries = context.selectedEntries, onFinished = onFinished)
        null -> Unit
    }

    DestinationPickerCase(context = context, viewModel = viewModel)
}

@Composable
private fun DestinationPickerCase(
    context: DialogsContext,
    viewModel: BrowseViewModel,
) {
    if (!context.pickerState.visible) return
    val titleRes =
        when (context.pickerState.purpose) {
            PickerPurpose.COPY -> R.string.picker_title_copy
            PickerPurpose.UPLOAD -> R.string.picker_title_upload
            PickerPurpose.MOVE, null -> R.string.picker_title_move
        }
    DestinationPickerDialog(
        title = stringResource(titleRes),
        content =
            DestinationPickerContent(
                breadcrumbs = context.pickerState.breadcrumbs,
                folders = context.pickerState.folders,
                isLoading = context.pickerState.isLoading,
            ),
        actions =
            DestinationPickerActions(
                onNavigateInto = viewModel.picker::navigateInto,
                onNavigateToBreadcrumb = viewModel.picker::navigateToBreadcrumb,
                onConfirm = viewModel.picker::confirm,
                onDismiss = viewModel.picker::dismiss,
            ),
    )
}

@Composable
private fun TagPickerCase(
    context: DialogsContext,
    viewModel: BrowseViewModel,
    scope: CoroutineScope,
    onFinished: () -> Unit,
) {
    TagPickerDialog(
        tags = context.tags,
        onApply = { tagIds ->
            val uris = context.selectedEntries.map { it.uri }
            scope.launch { tagIds.forEach { tagId -> viewModel.metadataActions.applyTag(uris, tagId) } }
            onFinished()
        },
        onManageTags = {
            onFinished()
            context.onOpenManageTags()
        },
        onDismiss = onFinished,
    )
}

@Composable
private fun FavoriteCollectionPickerCase(
    context: DialogsContext,
    viewModel: BrowseViewModel,
    scope: CoroutineScope,
    onFinished: () -> Unit,
) {
    FavoriteCollectionPickerDialog(
        collections = context.favoriteCollections,
        onSelect = { collectionId ->
            scope.launch { viewModel.metadataActions.favoriteInto(context.selectedEntries, collectionId) }
            onFinished()
        },
        onDismiss = onFinished,
    )
}

@Composable
private fun AddToVaultCase(
    context: DialogsContext,
    viewModel: BrowseViewModel,
    onFinished: () -> Unit,
) {
    ConfirmDangerousActionDialog(
        title = stringResource(R.string.add_to_vault_confirm_title),
        message = stringResource(R.string.add_to_vault_confirm_message, context.selectedEntries.size),
        confirmLabel = stringResource(R.string.add_to_vault_confirm_button),
        onConfirm = {
            viewModel.addToVault(context.selectedEntries)
            onFinished()
        },
        onDismiss = onFinished,
    )
}

/** Launches the system share sheet once, then immediately clears the selection -- there's no confirm step, unlike the dialogs above. */
@Composable
private fun ShareCase(
    entries: List<FileEntry>,
    onFinished: () -> Unit,
) {
    val androidContext = LocalContext.current
    val chooserTitle = stringResource(R.string.share_chooser_title)
    LaunchedEffect(entries) {
        androidContext.startActivity(Intent.createChooser(ShareIntentFactory.createShareIntent(entries), chooserTitle))
        onFinished()
    }
}

@Composable
private fun RenameDialog(
    target: FileEntry?,
    viewModel: BrowseViewModel,
    onFinished: () -> Unit,
) {
    if (target == null) return
    TextInputDialog(
        title = stringResource(R.string.rename_title),
        confirmLabel = stringResource(R.string.rename_confirm),
        initialValue = target.name,
        onConfirm = { newName ->
            viewModel.rename(target, newName)
            onFinished()
        },
        onDismiss = onFinished,
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
