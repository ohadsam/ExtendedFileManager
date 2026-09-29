package com.efm.filemanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.domain.model.ViewMode

/** The view-mode quick toggle shared by any screen with a file list (Browse, Search, ...). */
@Composable
fun ViewModeMenu(
    expanded: Boolean,
    viewMode: ViewMode,
    onViewModeChanged: (ViewMode) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        ViewMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(stringResource(mode.labelRes())) },
                leadingIcon = { Icon(mode.icon(), contentDescription = null) },
                trailingIcon = {
                    if (mode == viewMode) Icon(Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    onDismiss()
                    onViewModeChanged(mode)
                },
            )
        }
    }
}
