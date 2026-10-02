package com.efm.filemanager.ui.feature.audit

import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import com.efm.filemanager.ui.components.query.SelectableMenuItem

@Composable
internal fun AuditFilterMenu(
    expanded: Boolean,
    selected: AuditFilter,
    onSelect: (AuditFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        AuditFilter.entries.forEach { filter ->
            SelectableMenuItem(
                labelRes = filter.labelRes,
                selected = filter == selected,
                onDismiss = onDismiss,
                onClick = { onSelect(filter) },
            )
        }
    }
}
