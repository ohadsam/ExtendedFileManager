package com.efm.filemanager.ui.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog
import com.efm.filemanager.ui.components.TextInputDialog

internal sealed interface FavoritesDialog {
    data class CreateCollection(val parentId: Long?) : FavoritesDialog

    data class Rename(val collection: FavoriteCollection) : FavoritesDialog

    data class Delete(val collection: FavoriteCollection) : FavoritesDialog
}

@Composable
internal fun FavoritesDialogHost(
    dialog: FavoritesDialog?,
    viewModel: FavoritesViewModel,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is FavoritesDialog.CreateCollection ->
            TextInputDialog(
                title = stringResource(R.string.favorites_new_collection),
                confirmLabel = stringResource(R.string.create_entry_confirm),
                onConfirm = { name ->
                    viewModel.createCollection(name, dialog.parentId)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        is FavoritesDialog.Rename ->
            TextInputDialog(
                title = stringResource(R.string.rename_title),
                confirmLabel = stringResource(R.string.rename_confirm),
                initialValue = dialog.collection.name,
                onConfirm = { name ->
                    viewModel.renameCollection(dialog.collection, name)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        is FavoritesDialog.Delete ->
            ConfirmDangerousActionDialog(
                title = stringResource(R.string.favorites_delete_collection_title),
                message = stringResource(R.string.favorites_delete_collection_message, dialog.collection.name),
                confirmLabel = stringResource(R.string.action_delete),
                onConfirm = {
                    viewModel.deleteCollection(dialog.collection)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        null -> Unit
    }
}
