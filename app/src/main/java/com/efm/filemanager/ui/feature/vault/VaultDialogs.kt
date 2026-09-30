package com.efm.filemanager.ui.feature.vault

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog

@Composable
internal fun VaultRemoveConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDangerousActionDialog(
        title = stringResource(R.string.vault_remove_confirm_title),
        message = stringResource(R.string.vault_remove_confirm_message, count),
        confirmLabel = stringResource(R.string.vault_remove_confirm_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun VaultSnackbarEffect(
    viewModel: VaultViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val wrongPasswordMessage = stringResource(R.string.vault_wrong_password)
    val passwordsDontMatchMessage = stringResource(R.string.vault_passwords_dont_match)
    val operationFailedMessage = stringResource(R.string.operation_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                VaultEvent.WrongPassword -> snackbarHostState.showSnackbar(wrongPasswordMessage)
                VaultEvent.PasswordsDoNotMatch -> snackbarHostState.showSnackbar(passwordsDontMatchMessage)
                VaultEvent.OperationFailed -> snackbarHostState.showSnackbar(operationFailedMessage)
            }
        }
    }
}
