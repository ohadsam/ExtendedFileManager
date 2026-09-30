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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.VaultEntry
import com.efm.filemanager.ui.feature.browse.DestinationPickerActions
import com.efm.filemanager.ui.feature.browse.DestinationPickerContent
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

    VaultScaffold(
        inputs = VaultScaffoldInputs(isUnlocked, biometricEnabled, entries, selectedIds, onOpenDrawer, viewModel),
        snackbarHostState = snackbarHostState,
        onShowRemoveConfirm = { showRemoveConfirm = true },
    )

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

private data class VaultScaffoldInputs(
    val isUnlocked: Boolean,
    val biometricEnabled: Boolean,
    val entries: List<VaultEntry>,
    val selectedIds: SnapshotStateList<Long>,
    val onOpenDrawer: () -> Unit,
    val viewModel: VaultViewModel,
)

@Composable
private fun VaultScaffold(
    inputs: VaultScaffoldInputs,
    snackbarHostState: SnackbarHostState,
    onShowRemoveConfirm: () -> Unit,
) {
    val selectedIds = inputs.selectedIds
    val viewModel = inputs.viewModel
    Scaffold(
        topBar = {
            VaultTopBar(
                state = VaultTopBarState(isUnlocked = inputs.isUnlocked, biometricEnabled = inputs.biometricEnabled),
                actions =
                    VaultTopBarActions(
                        onOpenDrawer = inputs.onOpenDrawer,
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
                onRemove = onShowRemoveConfirm,
            )
        },
    ) { innerPadding ->
        if (inputs.isUnlocked) {
            VaultListBody(entries = inputs.entries, selectedIds = selectedIds, modifier = Modifier.padding(innerPadding))
        } else {
            VaultGateBody(
                state = VaultGateState(isPasswordSet = viewModel.isPasswordSet, biometricEnabled = inputs.biometricEnabled),
                actions =
                    VaultGateActions(
                        onSetPassword = viewModel::setPassword,
                        onUnlock = viewModel::unlockWithPassword,
                        onBiometricUnlock = viewModel::unlockWithBiometric,
                    ),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
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
        content = DestinationPickerContent(pickerState.breadcrumbs, pickerState.folders, pickerState.isLoading),
        actions = DestinationPickerActions(picker::navigateInto, picker::navigateToBreadcrumb, picker::confirm, picker::dismiss),
    )
}
