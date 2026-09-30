package com.efm.filemanager.ui.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.ui.feature.browse.DestinationPickerDialog

@Composable
fun VaultScreen(
    onOpenDrawer: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    VaultSnackbarEffect(viewModel = viewModel, snackbarHostState = snackbarHostState)

    Scaffold(
        topBar = {
            VaultTopBar(
                state = VaultTopBarState(isUnlocked = isUnlocked, biometricEnabled = biometricEnabled),
                actions =
                    VaultTopBarActions(
                        onOpenDrawer = onOpenDrawer,
                        onLock = {
                            selectedIds.clear()
                            viewModel.lock()
                        },
                        onSetBiometricEnabled = viewModel::setBiometricEnabled,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            VaultSelectionFabs(
                visible = selectedIds.isNotEmpty(),
                onExport = {
                    viewModel.exportPicker.open(selectedIds.toList())
                    selectedIds.clear()
                },
                onRemove = { showRemoveConfirm = true },
            )
        },
    ) { innerPadding ->
        if (isUnlocked) {
            VaultListBody(entries = entries, selectedIds = selectedIds, modifier = Modifier.padding(innerPadding))
        } else {
            VaultGateBody(
                isPasswordSet = viewModel.isPasswordSet,
                biometricEnabled = biometricEnabled,
                onSetPassword = viewModel::setPassword,
                onUnlock = viewModel::unlockWithPassword,
                onBiometricUnlock = viewModel::unlockWithBiometric,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showRemoveConfirm) {
        VaultRemoveConfirmDialog(
            count = selectedIds.size,
            onConfirm = {
                viewModel.removeEntries(selectedIds.toList())
                selectedIds.clear()
                showRemoveConfirm = false
            },
            onDismiss = { showRemoveConfirm = false },
        )
    }

    VaultExportPickerDialog(viewModel.exportPicker)
}

@Composable
private fun VaultSelectionFabs(
    visible: Boolean,
    onExport: () -> Unit,
    onRemove: () -> Unit,
) {
    if (!visible) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatingActionButton(onClick = onExport) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.vault_export_action))
        }
        ExtendedFloatingActionButton(
            onClick = onRemove,
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            text = { Text(stringResource(R.string.vault_remove_confirm_button)) },
        )
    }
}

@Composable
private fun VaultExportPickerDialog(picker: VaultExportPickerController) {
    val pickerState by picker.pickerState.collectAsStateWithLifecycle()
    if (!pickerState.visible) return
    DestinationPickerDialog(
        title = stringResource(R.string.vault_export_action),
        breadcrumbs = pickerState.breadcrumbs,
        folders = pickerState.folders,
        isLoading = pickerState.isLoading,
        onNavigateInto = picker::navigateInto,
        onNavigateToBreadcrumb = picker::navigateToBreadcrumb,
        onConfirm = picker::confirm,
        onDismiss = picker::dismiss,
    )
}
