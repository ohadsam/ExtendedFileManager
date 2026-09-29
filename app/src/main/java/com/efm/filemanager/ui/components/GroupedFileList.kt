package com.efm.filemanager.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efm.filemanager.domain.model.FileGroup
import com.efm.filemanager.domain.model.GroupKey
import com.efm.filemanager.domain.model.ViewMode
import com.efm.filemanager.ui.components.query.displayLabel

private val GRID_CELL_MIN_SIZE = 96.dp

/** Renders [groups] in [viewMode] with a header per non-empty [GroupKey] -- shared by Browse and Search. */
@Composable
fun GroupedFileList(
    modifier: Modifier = Modifier,
    groups: List<FileGroup>,
    selectedUris: List<Uri>,
    viewMode: ViewMode = ViewMode.LIST,
    actions: FileEntryActions,
) {
    if (viewMode == ViewMode.GRID) {
        FileGrid(modifier = modifier, groups = groups, selectedUris = selectedUris, actions = actions)
    } else {
        FileColumn(modifier = modifier, groups = groups, selectedUris = selectedUris, viewMode = viewMode, actions = actions)
    }
}

@Composable
private fun FileColumn(
    modifier: Modifier,
    groups: List<FileGroup>,
    selectedUris: List<Uri>,
    viewMode: ViewMode,
    actions: FileEntryActions,
) {
    val density = viewMode.rowDensity()
    LazyColumn(modifier = modifier.fillMaxSize()) {
        groups.forEach { group ->
            if (group.key != GroupKey.None) {
                item(key = "header_${group.key}") { GroupHeader(group.key) }
            }
            items(group.files, key = { it.uri.toString() }) { entry ->
                FileRow(
                    entry = entry,
                    isSelected = selectedUris.contains(entry.uri),
                    onClick = { actions.onClick(entry) },
                    onLongClick = { actions.onLongClick(entry) },
                    density = density,
                )
            }
        }
    }
}

@Composable
private fun FileGrid(
    modifier: Modifier,
    groups: List<FileGroup>,
    selectedUris: List<Uri>,
    actions: FileEntryActions,
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE), modifier = modifier.fillMaxSize()) {
        groups.forEach { group ->
            if (group.key != GroupKey.None) {
                item(key = "header_${group.key}", span = { GridItemSpan(maxLineSpan) }) { GroupHeader(group.key) }
            }
            items(group.files, key = { it.uri.toString() }) { entry ->
                FileGridCell(
                    entry = entry,
                    isSelected = selectedUris.contains(entry.uri),
                    onClick = { actions.onClick(entry) },
                    onLongClick = { actions.onLongClick(entry) },
                )
            }
        }
    }
}

private fun ViewMode.rowDensity(): FileRowDensity =
    when (this) {
        ViewMode.COMPACT -> FileRowDensity.COMPACT
        ViewMode.DETAILED -> FileRowDensity.DETAILED
        ViewMode.LIST, ViewMode.GRID -> FileRowDensity.NORMAL
    }

@Composable
private fun GroupHeader(key: GroupKey) {
    Text(
        text = key.displayLabel(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
