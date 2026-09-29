package com.efm.filemanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R

/**
 * The tag/favorite/lock quick-actions any selection bar can offer (Browse, Search, Duplicates,
 * Preview -- docs/PLAN.md Phase 9). [MetadataMenuItems] is the bare items, reusable inside a
 * screen's own existing dropdown; [MetadataQuickActionsMenu] wraps them in their own dropdown
 * for a screen that doesn't have one yet.
 */
@Composable
fun MetadataMenuItems(
    onAddTag: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_add_tag)) },
        leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) },
        onClick = onAddTag,
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_toggle_favorite)) },
        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
        onClick = onToggleFavorite,
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_toggle_lock)) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        onClick = onToggleLock,
    )
}

@Composable
fun MetadataQuickActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: MetadataQuickActions,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val onShowDetails = actions.onShowDetails
        if (onShowDetails != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.file_details_title)) },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onShowDetails()
                },
            )
        }
        MetadataMenuItems(
            onAddTag = {
                onDismiss()
                actions.onAddTag()
            },
            onToggleFavorite = {
                onDismiss()
                actions.onToggleFavorite()
            },
            onToggleLock = {
                onDismiss()
                actions.onToggleLock()
            },
        )
    }
}
