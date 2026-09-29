package com.efm.filemanager.ui.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileGroup
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.previewType
import com.efm.filemanager.domain.query.groupResult
import com.efm.filemanager.ui.components.GroupedFileList
import com.efm.filemanager.ui.components.query.FilterMenu
import com.efm.filemanager.ui.components.query.SortGroupMenu

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onOpenLocation: (FileEntry) -> Unit,
    onOpenPreview: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups = remember(uiState.results, uiState.querySpec) { uiState.querySpec.groupResult(uiState.results) }

    Scaffold(
        topBar = {
            SearchTopBar(
                querySpec = uiState.querySpec,
                onQuerySpecChanged = viewModel::updateQuerySpec,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        SearchBody(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            groups = groups,
            onResultClick = { entry -> onSearchResultTapped(entry, viewModel, onOpenLocation, onOpenPreview) },
        )
    }
}

private fun onSearchResultTapped(
    entry: FileEntry,
    viewModel: SearchViewModel,
    onOpenLocation: (FileEntry) -> Unit,
    onOpenPreview: () -> Unit,
) {
    if (entry.isDirectory || entry.previewType() == PreviewType.NONE) {
        onOpenLocation(entry)
    } else {
        viewModel.openPreview(entry)
        onOpenPreview()
    }
}

@Composable
private fun SearchTopBar(
    querySpec: QuerySpec,
    onQuerySpecChanged: (QuerySpec) -> Unit,
    onNavigateBack: () -> Unit,
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
private fun SearchBody(
    modifier: Modifier = Modifier,
    uiState: SearchUiState,
    groups: List<FileGroup>,
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
                selectedUris = emptyList(),
                onEntryClick = onResultClick,
                onEntryLongClick = {},
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
