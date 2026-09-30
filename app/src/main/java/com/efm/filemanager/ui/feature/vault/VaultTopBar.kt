package com.efm.filemanager.ui.feature.vault

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.efm.filemanager.R

internal data class VaultTopBarState(
    val isUnlocked: Boolean,
    val biometricEnabled: Boolean,
)

internal data class VaultTopBarActions(
    val onOpenDrawer: () -> Unit,
    val onLock: () -> Unit,
    val onSetBiometricEnabled: (Boolean) -> Unit,
)

@Composable
internal fun VaultTopBar(
    state: VaultTopBarState,
    actions: VaultTopBarActions,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = actions.onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_vault)) },
        actions = { if (state.isUnlocked) VaultTopBarUnlockedActions(state = state, actions = actions) },
    )
}

@Composable
private fun VaultTopBarUnlockedActions(
    state: VaultTopBarState,
    actions: VaultTopBarActions,
) {
    val context = LocalContext.current
    val canUseBiometric = remember { biometricAvailable(context) }
    var menuExpanded by remember { mutableStateOf(false) }

    IconButton(onClick = actions.onLock) {
        Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.vault_lock_action))
    }
    if (canUseBiometric) {
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
            }
            VaultMoreMenu(
                expanded = menuExpanded,
                biometricEnabled = state.biometricEnabled,
                onSetBiometricEnabled = actions.onSetBiometricEnabled,
                onDismiss = { menuExpanded = false },
            )
        }
    }
}

@Composable
private fun VaultMoreMenu(
    expanded: Boolean,
    biometricEnabled: Boolean,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val promptTitle = stringResource(R.string.vault_enable_biometric)
    val cancelLabel = stringResource(R.string.close)
    val label = stringResource(if (biometricEnabled) R.string.vault_disable_biometric else R.string.vault_enable_biometric)

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(label) },
            onClick = {
                onDismiss()
                toggleBiometric(context, biometricEnabled, promptTitle, cancelLabel, onSetBiometricEnabled)
            },
        )
    }
}

/** Disabling never needs a fresh biometric check; enabling does, as a "confirm it's really you" step before turning it on. */
private fun toggleBiometric(
    context: Context,
    currentlyEnabled: Boolean,
    promptTitle: String,
    cancelLabel: String,
    onSetBiometricEnabled: (Boolean) -> Unit,
) {
    if (currentlyEnabled) {
        onSetBiometricEnabled(false)
        return
    }
    val activity = context as? FragmentActivity ?: return
    showBiometricPrompt(activity, promptTitle, cancelLabel, onSuccess = { onSetBiometricEnabled(true) }, onError = {})
}
