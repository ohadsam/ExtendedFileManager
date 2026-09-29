package com.efm.filemanager.ui.feature.search

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileGroup
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.previewType
import com.efm.filemanager.domain.query.groupResult
import com.efm.filemanager.ui.components.FileEntryActions
import com.efm.filemanager.ui.components.GroupedFileList
import com.efm.filemanager.ui.components.MetadataQuickActionsMenu
import com.efm.filemanager.ui.components.TagPickerDialog
import com.efm.filemanager.ui.components.query.FilterMenu
import com.efm.filemanager.ui.components.query.SortGroupMenu
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onOpenLocation: (FileEntry) -> Unit,
    onOpenPreview: () -> Unit,
    onOpenManageTags: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val groups = remember(uiState.results, uiState.querySpec) { uiState.querySpec.groupResult(uiState.results) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val selectedEntries = uiState.results.filter { selectedUris.contains(it.uri) }
    var tagPickerVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (selectedUris.isEmpty()) {
                SearchTopBar(
                    querySpec = uiState.querySpec,
                    onQuerySpecChanged = viewModel::updateQuerySpec,
                    onNavigateBack = onNavigateBack,
                    tags = tags,
                )
            } else {
                SearchSelectionTopBar(
                    count = selectedUris.size,
                    onClose = { selectedUris.clear() },
                    onAddTag = { tagPickerVisible = true },
                    onToggleFavorite = { scope.launch { viewModel.metadataActions.toggleFavorite(selectedEntries) } },
                    onToggleLock = { scope.launch { viewModel.metadataActions.toggleLock(selectedEntries) } },
                )
            }
        },
    ) { innerPadding ->
        SearchBody(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            groups = groups,
            selectedUris = selectedUris,
            onResultClick = { entry -> onSearchResultTapped(entry, selectedUris, viewModel, onOpenLocation, onOpenPreview) },
        )
    }

    if (tagPickerVisible) {
        TagPickerDialog(
            tags = tags,
            onApply = { tagIds ->
                val uris = selectedEntries.map { it.uri }
                scope.launch { tagIds.forEach { tagId -> viewModel.metadataActions.applyTag(uris, tagId) } }
                tagPickerVisible = false
                selectedUris.clear()
            },
            onManageTags = {
                tagPickerVisible = false
                onOpenManageTags()
            },
            onDismiss = { tagPickerVisible = false },
        )
    }
}

private fun onSearchResultTapped(
    entry: FileEntry,
    selectedUris: SnapshotStateList<Uri>,
    viewModel: SearchViewModel,
    onOpenLocation: (FileEntry) -> Unit,
    onOpenPreview: () -> Unit,
) {
    if (selectedUris.isNotEmpty()) {
        toggleSelection(selectedUris, entry.uri)
    } else if (entry.isDirectory || entry.previewType() == PreviewType.NONE) {
        onOpenLocation(entry)
    } else {
        viewModel.openPreview(entry)
        onOpenPreview()
    }
}

private fun toggleSelection(
    selected: SnapshotStateList<Uri>,
    uri: Uri,
) {
    if (selected.contains(uri)) selected.remove(uri) else selected.add(uri)
}

@Composable
private fun SearchTopBar(
    querySpec: QuerySpec,
    onQuerySpecChanged: (QuerySpec) -> Unit,
    onNavigateBack: () -> Unit,
    tags: List<FileTag>,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
        },
        title = {
            TextField(
                value = querySpec.freeText,
                onValueChange = { text -> onQuerySpecChanged(querySpec.copy(freeText = text)) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
            )
        },
        actions = {
            Box {
                IconButton(onClick = { filterMenuExpanded = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.filter_menu))
                }
                FilterMenu(
                    expanded = filterMenuExpanded,
                    spec = querySpec,
                    onSpecChanged = onQuerySpecChanged,
                    onDismiss = { filterMenuExpanded = false },
                    tags = tags,
                )
            }
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.sort_menu))
                }
                SortGroupMenu(
                    expanded = sortMenuExpanded,
                    spec = querySpec,
                    onSpecChanged = onQuerySpecChanged,
                    onDismiss = { sortMenuExpanded = false },
                )
            }
        },
    )
}

@Composable
private fun SearchSelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onAddTag: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.selection_clear))
            }
        },
        title = { Text(stringResource(R.string.selection_count, count)) },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
                }
                MetadataQuickActionsMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onAddTag = onAddTag,
                    onToggleFavorite = onToggleFavorite,
                    onToggleLock = onToggleLock,
                )
            }
        },
    )
}

@Composable
private fun SearchBody(
    modifier: Modifier = Modifier,
    uiState: SearchUiState,
    groups: List<FileGroup>,
    selectedUris: SnapshotStateList<Uri>,
    onResultClick: (FileEntry) -> Unit,
) {
    when {
        uiState.isIndexing && uiState.results.isEmpty() -> SearchMessageState(modifier, R.string.search_indexing, showSpinner = true)
        !uiState.hasSearched -> SearchMessageState(modifier, R.string.search_empty_prompt)
        groups.sumOf { it.files.size } == 0 -> SearchMessageState(modifier, R.string.search_no_results)
        else ->
            GroupedFileList(
                modifier = modifier,
                groups = groups,
                selectedUris = selectedUris,
                viewMode = uiState.viewMode,
                actions = FileEntryActions(onResultClick, { entry -> toggleSelection(selectedUris, entry.uri) }),
            )
    }
}

@Composable
private fun SearchMessageState(
    modifier: Modifier,
    messageRes: Int,
    showSpinner: Boolean = false,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showSpinner) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
            }
            Text(stringResource(messageRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
