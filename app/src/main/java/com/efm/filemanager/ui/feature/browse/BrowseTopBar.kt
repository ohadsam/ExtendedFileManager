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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
            viewModel.openMovePicker(selectedEntries)
            selectedUris.clear()
        },
        onCopy = {
            viewModel.openCopyPicker(selectedEntries)
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
    breadcrumbs: List<BreadcrumbEntry>,
    selectionBarState: SelectionBarState,
    onOpenDrawer: () -> Unit,
    onNavigateToBreadcrumb: (Int) -> Unit,
) {
    if (selectionBarState.selectedCount > 0) {
        SelectionTopAppBar(state = selectionBarState)
    } else {
        BrowseNormalTopBar(breadcrumbs = breadcrumbs, onOpenDrawer = onOpenDrawer, onNavigateToBreadcrumb = onNavigateToBreadcrumb)
    }
}

@Composable
private fun BrowseNormalTopBar(
    breadcrumbs: List<BreadcrumbEntry>,
    onOpenDrawer: () -> Unit,
    onNavigateToBreadcrumb: (Int) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { BreadcrumbBar(breadcrumbs = breadcrumbs, onCrumbClick = onNavigateToBreadcrumb) },
        actions = {
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.sort_menu))
                }
                // Stub: the menu's grouping/subheadings are real, but switching sort
                // order isn't wired up until Phase 5's filter/sort/group engine lands.
                SortDropdownMenu(expanded = sortMenuExpanded, onDismiss = { sortMenuExpanded = false })
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
private fun SortDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuSectionHeader(stringResource(R.string.sort_section_sort_by))
        DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_name)) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_date)) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_size)) }, onClick = onDismiss)
        HorizontalDivider()
        DropdownMenuSectionHeader(stringResource(R.string.sort_section_order))
        DropdownMenuItem(text = { Text(stringResource(R.string.sort_order_ascending)) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text(stringResource(R.string.sort_order_descending)) }, onClick = onDismiss)
    }
}

@Composable
private fun DropdownMenuSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
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
