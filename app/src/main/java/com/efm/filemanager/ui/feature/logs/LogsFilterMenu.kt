package com.efm.filemanager.ui.feature.logs

import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import com.efm.filemanager.ui.components.query.SelectableMenuItem

@Composable
internal fun LogsFilterMenu(
    expanded: Boolean,
    selected: LogPriorityFilter,
    onSelect: (LogPriorityFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        LogPriorityFilter.entries.forEach { filter ->
            SelectableMenuItem(
                labelRes = filter.labelRes,
                selected = filter == selected,
                onDismiss = onDismiss,
                onClick = { onSelect(filter) },
            )
        }
    }
}
