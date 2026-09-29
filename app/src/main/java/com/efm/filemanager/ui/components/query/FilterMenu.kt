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
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.FileTypeFilter
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.SizePreset

/** Type / category / size / date / tag / favorite / locked filters, wired to a real [QuerySpec] -- shared by Browse and Search. */
@Composable
fun FilterMenu(
    expanded: Boolean,
    spec: QuerySpec,
    onSpecChanged: (QuerySpec) -> Unit,
    onDismiss: () -> Unit,
    tags: List<FileTag> = emptyList(),
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
        QueryMenuSectionHeader(stringResource(R.string.filter_section_status))
        ToggleMenuItem(stringResource(R.string.filter_favorites_only), spec.favoriteOnly) {
            onSpecChanged(spec.copy(favoriteOnly = !spec.favoriteOnly))
        }
        ToggleMenuItem(stringResource(R.string.filter_locked_only), spec.lockedOnly) {
            onSpecChanged(spec.copy(lockedOnly = !spec.lockedOnly))
        }
        if (tags.isNotEmpty()) {
            HorizontalDivider()
            QueryMenuSectionHeader(stringResource(R.string.filter_section_tags))
            tags.forEach { tag ->
                ToggleMenuItem(tag.name, tag.id in spec.tagIds) {
                    val updated = if (tag.id in spec.tagIds) spec.tagIds - tag.id else spec.tagIds + tag.id
                    onSpecChanged(spec.copy(tagIds = updated))
                }
            }
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
