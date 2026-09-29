package com.efm.filemanager.ui.components.query

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Shared building blocks for [SortGroupMenu] and [FilterMenu]'s sectioned `DropdownMenu`s. */
@Composable
internal fun QueryMenuSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
internal fun SelectableMenuItem(
    labelRes: Int,
    selected: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val checkIcon: (@Composable () -> Unit)? = if (selected) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        leadingIcon = checkIcon,
        onClick = {
            onDismiss()
            onClick()
        },
    )
}

/** A menu item that toggles a state and stays open -- unlike [SelectableMenuItem], which picks one option and closes. */
@Composable
internal fun ToggleMenuItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = onToggle,
    )
}
