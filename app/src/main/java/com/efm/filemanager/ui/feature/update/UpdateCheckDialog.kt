package com.efm.filemanager.ui.feature.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R

/** One dialog per [UpdateCheckState] -- usually [UpdateCheckState.Hidden], in which case this renders nothing. */
@Composable
fun UpdateCheckDialog(
    state: UpdateCheckState,
    actions: UpdateCheckActions,
) {
    when (state) {
        UpdateCheckState.Hidden -> Unit
        UpdateCheckState.Checking -> CheckingDialog(actions.onDismiss)
        UpdateCheckState.UpToDate -> MessageDialog(stringResource(R.string.update_up_to_date), actions.onDismiss)
        is UpdateCheckState.Available -> AvailableDialog(state, actions)
        UpdateCheckState.NeedsInstallPermission -> NeedsPermissionDialog(actions)
        UpdateCheckState.Downloading -> CheckingDialog(actions.onDismiss, titleRes = R.string.update_downloading)
        UpdateCheckState.ReadyToInstall -> ReadyToInstallDialog(actions)
        is UpdateCheckState.Failed -> MessageDialog(stringResource(state.reason.messageRes()), actions.onDismiss)
    }
}

/** Bundled so [UpdateCheckDialog] stays under detekt's `LongParameterList` threshold. */
data class UpdateCheckActions(
    val onDownloadClick: () -> Unit,
    val onRequestInstallPermission: () -> Unit,
    val onInstallClick: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
private fun CheckingDialog(
    onDismiss: () -> Unit,
    titleRes: Int = R.string.update_checking,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) },
        confirmButton = {},
    )
}

@Composable
private fun MessageDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_got_it_action)) } },
    )
}

@Composable
private fun AvailableDialog(
    state: UpdateCheckState.Available,
    actions: UpdateCheckActions,
) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(R.string.update_available_title, state.versionName)) },
        text = {
            Column {
                if (state.releaseNotes.isNotBlank()) Text(state.releaseNotes)
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onDownloadClick) { Text(stringResource(R.string.update_download_action)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.update_later_action)) }
        },
    )
}

@Composable
private fun NeedsPermissionDialog(actions: UpdateCheckActions) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(R.string.update_permission_title)) },
        text = { Text(stringResource(R.string.update_permission_message)) },
        confirmButton = {
            TextButton(onClick = actions.onRequestInstallPermission) { Text(stringResource(R.string.update_permission_open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.update_permission_cancel)) }
        },
    )
}

@Composable
private fun ReadyToInstallDialog(actions: UpdateCheckActions) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(R.string.update_ready_title)) },
        text = { Text(stringResource(R.string.update_ready_message)) },
        confirmButton = {
            TextButton(onClick = actions.onInstallClick) { Text(stringResource(R.string.update_install_action)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.update_later_action)) }
        },
    )
}
