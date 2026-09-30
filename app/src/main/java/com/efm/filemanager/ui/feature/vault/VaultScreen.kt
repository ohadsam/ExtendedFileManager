package com.efm.filemanager.ui.feature.vault

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R

@Composable
fun VaultScreen(
    onOpenDrawer: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    VaultSnackbarEffect(viewModel = viewModel, snackbarHostState = snackbarHostState)

    Scaffold(
        topBar = {
            VaultTopBar(
                onOpenDrawer = onOpenDrawer,
                isUnlocked = isUnlocked,
                onLock = {
                    selectedIds.clear()
                    viewModel.lock()
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { VaultRemoveFab(visible = selectedIds.isNotEmpty(), onClick = { showRemoveConfirm = true }) },
    ) { innerPadding ->
        if (isUnlocked) {
            VaultListBody(entries = entries, selectedIds = selectedIds, modifier = Modifier.padding(innerPadding))
        } else {
            VaultGateBody(
                isPasswordSet = viewModel.isPasswordSet,
                onSetPassword = viewModel::setPassword,
                onUnlock = viewModel::unlockWithPassword,
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
}

@Composable
private fun VaultTopBar(
    onOpenDrawer: () -> Unit,
    isUnlocked: Boolean,
    onLock: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_vault)) },
        actions = {
            if (isUnlocked) {
                IconButton(onClick = onLock) {
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.vault_lock_action))
                }
            }
        },
    )
}

@Composable
private fun VaultRemoveFab(
    visible: Boolean,
    onClick: () -> Unit,
) {
    if (!visible) return
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        text = { Text(stringResource(R.string.vault_remove_confirm_button)) },
    )
}
