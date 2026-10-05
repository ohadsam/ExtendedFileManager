package com.efm.filemanager.ui.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
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
        null -> Unit
    }
}
