package com.efm.filemanager.ui.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog
import com.efm.filemanager.ui.components.TextInputDialog

@Composable
internal fun DashboardDialogHost(
    dialog: DashboardDialog?,
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        DashboardDialog.SaveLayout ->
            TextInputDialog(
                title = stringResource(R.string.dashboard_save_layout_title),
                confirmLabel = stringResource(R.string.dashboard_save_layout_confirm),
                onConfirm = { name ->
                    viewModel.saveCurrentAsLayout(name)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        is DashboardDialog.RenameLayout ->
            TextInputDialog(
                title = stringResource(R.string.rename_title),
                confirmLabel = stringResource(R.string.rename_confirm),
                initialValue = dialog.layout.name,
                onConfirm = { name ->
                    viewModel.renameSavedLayout(dialog.layout, name)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        is DashboardDialog.DeleteLayout ->
            ConfirmDangerousActionDialog(
                title = stringResource(R.string.dashboard_delete_layout_title),
                message = stringResource(R.string.dashboard_delete_layout_message, dialog.layout.name),
                confirmLabel = stringResource(R.string.action_delete),
                onConfirm = {
                    viewModel.deleteSavedLayout(dialog.layout)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        null -> Unit
    }
}
