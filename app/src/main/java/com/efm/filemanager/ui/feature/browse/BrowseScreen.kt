package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry

@Composable
fun BrowseScreen(
    onOpenDrawer: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val treePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) viewModel.onTreeGranted(uri)
        }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val selectedUris = remember { mutableStateListOf<Uri>() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
                    }
                },
                title = {
                    BreadcrumbBar(
                        breadcrumbs = uiState.breadcrumbs,
                        onCrumbClick = viewModel::navigateToBreadcrumb,
                    )
                },
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
        },
    ) { innerPadding ->
        when {
            !uiState.hasAccess ->
                GrantAccessEmptyState(
                    modifier = Modifier.padding(innerPadding),
                    onGrantClick = { treePickerLauncher.launch(null) },
                )
            uiState.isLoading && uiState.files.isEmpty() -> LoadingState(Modifier.padding(innerPadding))
            uiState.files.isEmpty() -> EmptyFolderState(Modifier.padding(innerPadding))
            else ->
                FileList(
                    modifier = Modifier.padding(innerPadding),
                    files = uiState.files,
                    selectedUris = selectedUris,
                    onEntryClick = { entry ->
                        if (selectedUris.isNotEmpty()) {
                            toggleSelection(selectedUris, entry.uri)
                        } else if (entry.isDirectory) {
                            viewModel.openEntry(entry)
                        }
                    },
                    onEntryLongClick = { entry -> toggleSelection(selectedUris, entry.uri) },
                )
        }
    }
}

private fun toggleSelection(
    selected: SnapshotStateList<Uri>,
    uri: Uri,
) {
    if (selected.contains(uri)) selected.remove(uri) else selected.add(uri)
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

@Composable
private fun FileList(
    modifier: Modifier = Modifier,
    files: List<FileEntry>,
    selectedUris: List<Uri>,
    onEntryClick: (FileEntry) -> Unit,
    onEntryLongClick: (FileEntry) -> Unit,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(files, key = { it.uri.toString() }) { entry ->
            FileRow(
                entry = entry,
                isSelected = selectedUris.contains(entry.uri),
                onClick = { onEntryClick(entry) },
                onLongClick = { onEntryLongClick(entry) },
            )
        }
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = fileMetaLabel(entry),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun fileMetaLabel(entry: FileEntry): String {
    val dateLabel = formatDate(entry.lastModified)
    if (entry.isDirectory) return dateLabel
    return "${formatFileSize(entry.size)} • $dateLabel"
}

@Composable
private fun GrantAccessEmptyState(
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(stringResource(R.string.grant_access_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.grant_access_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onGrantClick) { Text(stringResource(R.string.grant_access_button)) }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyFolderState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.folder_empty))
    }
}
