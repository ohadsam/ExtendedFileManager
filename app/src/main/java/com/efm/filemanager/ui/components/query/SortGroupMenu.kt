package com.efm.filemanager.ui.components.query

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.GroupBy
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.SortField
import com.efm.filemanager.domain.model.SortOrder

/** Sort-by / order / group-by, wired to a real [QuerySpec] -- shared by Browse and Search. */
@Composable
fun SortGroupMenu(
    expanded: Boolean,
    spec: QuerySpec,
    onSpecChanged: (QuerySpec) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        QueryMenuSectionHeader(stringResource(R.string.sort_section_sort_by))
        SortField.entries.forEach { field ->
            SelectableMenuItem(field.labelRes(), spec.sortField == field, onDismiss) { onSpecChanged(spec.copy(sortField = field)) }
        }
        HorizontalDivider()
        QueryMenuSectionHeader(stringResource(R.string.sort_section_order))
        SortOrder.entries.forEach { order ->
            SelectableMenuItem(order.labelRes(), spec.sortOrder == order, onDismiss) { onSpecChanged(spec.copy(sortOrder = order)) }
        }
        HorizontalDivider()
        QueryMenuSectionHeader(stringResource(R.string.group_section_title))
        GroupBy.entries.forEach { groupBy ->
            SelectableMenuItem(groupBy.labelRes(), spec.groupBy == groupBy, onDismiss) { onSpecChanged(spec.copy(groupBy = groupBy)) }
        }
    }
}

private fun SortField.labelRes(): Int =
    when (this) {
        SortField.NAME -> R.string.sort_by_name
        SortField.DATE_MODIFIED -> R.string.sort_by_date
        SortField.SIZE -> R.string.sort_by_size
    }

private fun SortOrder.labelRes(): Int =
    when (this) {
        SortOrder.ASCENDING -> R.string.sort_order_ascending
        SortOrder.DESCENDING -> R.string.sort_order_descending
    }

private fun GroupBy.labelRes(): Int =
    when (this) {
        GroupBy.NONE -> R.string.group_none
        GroupBy.TYPE -> R.string.group_by_type
        GroupBy.SOURCE_APP -> R.string.group_by_source_app
        GroupBy.DATE_MODIFIED -> R.string.group_by_date
        GroupBy.TAG -> R.string.group_by_tag
    }
