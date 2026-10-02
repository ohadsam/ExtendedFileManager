package com.efm.filemanager.ui.feature.logs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog

@Composable
internal fun LogsClearConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDangerousActionDialog(
        title = stringResource(R.string.logs_clear_confirm_title),
        message = stringResource(R.string.logs_clear_confirm_message),
        confirmLabel = stringResource(R.string.logs_clear_confirm_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
