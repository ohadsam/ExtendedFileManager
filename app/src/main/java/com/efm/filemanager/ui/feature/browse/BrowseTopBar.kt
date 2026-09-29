package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.components.ViewModeMenu
import com.efm.filemanager.ui.components.icon
import com.efm.filemanager.ui.components.query.FilterMenu
import com.efm.filemanager.ui.components.query.SortGroupMenu

internal fun buildSelectionActions(
    selectedUris: SnapshotStateList<Uri>,
    selectedEntries: List<FileEntry>,
    viewModel: BrowseViewModel,
    onDialogRequested: (BrowseDialog) -> Unit,
): SelectionBarActions =
    SelectionBarActions(
        onClose = { selectedUris.clear() },
        onRename = { onDialogRequested(BrowseDialog.RENAME) },
        onMove = {
            viewModel.picker.openMovePicker(selectedEntries)
            selectedUris.clear()
        },
        onCopy = {
            viewModel.picker.openCopyPicker(selectedEntries)
            selectedUris.clear()
        },
        onDelete = { onDialogRequested(BrowseDialog.DELETE) },
    )

internal fun buildArchiveActions(
    onCompressRequested: () -> Unit,
    onExtractRequested: (replace: Boolean) -> Unit,
): ArchiveBarActions =
    ArchiveBarActions(
        onCompress = onCompressRequested,
        onExtract = { onExtractRequested(false) },
        onExtractAndReplace = { onExtractRequested(true) },
    )

@Composable
internal fun BrowseTopBar(
    state: BrowseTopBarState,
    actions: BrowseTopBarActions,
) {
    if (state.selectionBarState.selectedCount > 0) {
        SelectionTopAppBar(state = state.selectionBarState)
    } else {
        BrowseNormalTopBar(state = state, actions = actions)
    }
}

@Composable
private fun BrowseNormalTopBar(
    state: BrowseTopBarState,
    actions: BrowseTopBarActions,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var viewModeMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = actions.onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { BreadcrumbBar(breadcrumbs = state.breadcrumbs, onCrumbClick = actions.onNavigateToBreadcrumb) },
        actions = {
            IconButton(onClick = actions.onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_icon))
            }
            Box {
                IconButton(onClick = { viewModeMenuExpanded = true }) {
                    Icon(state.viewMode.icon(), contentDescription = stringResource(R.string.view_mode_menu))
                }
                ViewModeMenu(
                    expanded = viewModeMenuExpanded,
                    viewMode = state.viewMode,
                    onViewModeChanged = actions.onViewModeChanged,
                    onDismiss = { viewModeMenuExpanded = false },
                )
            }
            Box {
                IconButton(onClick = { filterMenuExpanded = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.filter_menu))
                }
                FilterMenu(
                    expanded = filterMenuExpanded,
                    spec = state.querySpec,
                    onSpecChanged = actions.onQuerySpecChanged,
                    onDismiss = { filterMenuExpanded = false },
                )
            }
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.sort_menu))
                }
                SortGroupMenu(
                    expanded = sortMenuExpanded,
                    spec = state.querySpec,
                    onSpecChanged = actions.onQuerySpecChanged,
                    onDismiss = { sortMenuExpanded = false },
                )
            }
        },
    )
}

@Composable
private fun SelectionTopAppBar(state: SelectionBarState) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val canRename = state.selectedCount == 1

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = state.actions.onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.selection_clear))
            }
        },
        title = { Text(stringResource(R.string.selection_count, state.selectedCount)) },
        actions = {
            if (canRename) {
                IconButton(onClick = state.actions.onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename_title))
                }
            }
            IconButton(onClick = state.actions.onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
            Box {
                IconButton(onClick = { moreMenuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
                }
                SelectionMoreMenu(expanded = moreMenuExpanded, state = state, onDismiss = { moreMenuExpanded = false })
            }
        },
    )
}

@Composable
private fun SelectionMoreMenu(
    expanded: Boolean,
    state: SelectionBarState,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_move)) },
            onClick = {
                onDismiss()
                state.actions.onMove()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_copy)) },
            onClick = {
                onDismiss()
                state.actions.onCopy()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_compress)) },
            onClick = {
                onDismiss()
                state.archiveActions.onCompress()
            },
        )
        if (state.canExtract) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_extract)) },
                onClick = {
                    onDismiss()
                    state.archiveActions.onExtract()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_extract_replace)) },
                onClick = {
                    onDismiss()
                    state.archiveActions.onExtractAndReplace()
                },
            )
        }
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<BreadcrumbEntry>,
    onCrumbClick: (Int) -> Unit,
) {
    if (breadcrumbs.isEmpty()) {
        Text(stringResource(R.string.app_name))
        return
    }
    LazyRow(verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(breadcrumbs) { index, crumb ->
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .clickable { onCrumbClick(index) }
                        .padding(horizontal = 4.dp),
            )
            if (index != breadcrumbs.lastIndex) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
