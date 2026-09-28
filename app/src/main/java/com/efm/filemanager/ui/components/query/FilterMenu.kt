package com.efm.filemanager.ui.components.query

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.DatePreset
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileTypeFilter
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.SizePreset

/** Type / category / size / date filters, wired to a real [QuerySpec] -- shared by Browse and Search. */
@Composable
fun FilterMenu(
    expanded: Boolean,
    spec: QuerySpec,
    onSpecChanged: (QuerySpec) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        QueryMenuSectionHeader(stringResource(R.string.filter_section_show))
        FileTypeFilter.entries.forEach { filter ->
            SelectableMenuItem(filter.labelRes(), spec.typeFilter == filter, onDismiss) { onSpecChanged(spec.copy(typeFilter = filter)) }
        }
        HorizontalDivider()
        QueryMenuSectionHeader(stringResource(R.string.filter_section_category))
        SelectableMenuItem(R.string.category_all, spec.category == null, onDismiss) { onSpecChanged(spec.copy(category = null)) }
        FileCategory.entries.forEach { category ->
            SelectableMenuItem(category.labelRes(), spec.category == category, onDismiss) {
                onSpecChanged(spec.copy(category = category))
            }
        }
        HorizontalDivider()
        QueryMenuSectionHeader(stringResource(R.string.filter_section_size))
        SizePreset.entries.forEach { preset ->
            SelectableMenuItem(preset.labelRes(), spec.sizePreset == preset, onDismiss) { onSpecChanged(spec.copy(sizePreset = preset)) }
        }
        HorizontalDivider()
        QueryMenuSectionHeader(stringResource(R.string.filter_section_date))
        DatePreset.entries.forEach { preset ->
            SelectableMenuItem(preset.labelRes(), spec.datePreset == preset, onDismiss) { onSpecChanged(spec.copy(datePreset = preset)) }
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.filter_clear)) },
            onClick = {
                onDismiss()
                onSpecChanged(spec.clearFilters())
            },
        )
    }
}
