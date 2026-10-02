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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog
import com.efm.filemanager.ui.components.FavoriteCollectionPickerDialog
import com.efm.filemanager.ui.components.FileEntryActions
import com.efm.filemanager.ui.components.GroupedFileList
import com.efm.filemanager.ui.components.MetadataMenuItems
import com.efm.filemanager.ui.components.MetadataQuickActions
import com.efm.filemanager.ui.components.TagPickerDialog
import com.efm.filemanager.ui.components.query.FilterMenu
import com.efm.filemanager.ui.components.query.SortGroupMenu
import com.efm.filemanager.ui.feature.filedetails.FileDetailsSheet
import kotlinx.coroutines.launch

/** [SearchScreen]'s own transient UI state -- kept off its composable's stack to keep the function short. */
private class SearchUiFlags {
    var tagPickerVisible by mutableStateOf(false)
    var detailsVisible by mutableStateOf(false)
    var collectionPickerVisible by mutableStateOf(false)
    var addToVaultConfirmVisible by mutableStateOf(false)
}

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onOpenLocation: (FileEntry) -> Unit,
    onOpenPreview: () -> Unit,
    onOpenManageTags: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups = remember(uiState.results, uiState.querySpec) { uiState.querySpec.groupResult(uiState.results) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val flags = remember { SearchUiFlags() }
    val snackbarHostState = remember { SnackbarHostState() }

    SearchSnackbarEffect(viewModel = viewModel, snackbarHostState = snackbarHostState)

    Scaffold(
        topBar = {
            if (selectedUris.isEmpty()) {
                val tags by viewModel.tags.collectAsStateWithLifecycle()
                SearchTopBar(
                    querySpec = uiState.querySpec,
                    onQuerySpecChanged = viewModel::updateQuerySpec,
                    onNavigateBack = onNavigateBack,
                    tags = tags,
                )
            } else {
                SearchSelectionBar(selectedUris = selectedUris, uiState = uiState, viewModel = viewModel, flags = flags)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SearchBody(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            groups = groups,
            selectedUris = selectedUris,
            onResultClick = { entry -> onSearchResultTapped(entry, selectedUris, viewModel, onOpenLocation, onOpenPreview) },
        )
    }

    SearchDialogsSection(
        flags = flags,
        selectedEntries = uiState.results.filter { selectedUris.contains(it.uri) },
        selectedUris = selectedUris,
        viewModel = viewModel,
        onOpenManageTags = onOpenManageTags,
    )
}

@Composable
private fun SearchSnackbarEffect(
    viewModel: SearchViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val operationFailedMessage = stringResource(R.string.operation_failed)
    val unlockLabel = stringResource(R.string.file_details_lock)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.AddToVaultBlockedByLock -> {
                    val message = context.getString(R.string.add_to_vault_blocked_by_lock, event.lockedEntries.size)
                    val result = snackbarHostState.showSnackbar(message = message, actionLabel = unlockLabel)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.metadataActions.unlock(event.lockedEntries.map { it.uri })
                    }
                }
                SearchEvent.OperationFailed -> snackbarHostState.showSnackbar(operationFailedMessage)
            }
        }
    }
}

@Composable
private fun SearchSelectionBar(
    selectedUris: SnapshotStateList<Uri>,
    uiState: SearchUiState,
    viewModel: SearchViewModel,
    flags: SearchUiFlags,
) {
    val selectedEntries = uiState.results.filter { selectedUris.contains(it.uri) }
    val scope = rememberCoroutineScope()
    val onShowDetails: (() -> Unit)? = if (selectedUris.size == 1) ({ flags.detailsVisible = true }) else null
    SearchSelectionTopBar(
        count = selectedUris.size,
        onClose = { selectedUris.clear() },
        onAddToVault = { flags.addToVaultConfirmVisible = true },
        actions =
            MetadataQuickActions(
                onAddTag = { flags.tagPickerVisible = true },
                onToggleFavorite = {
                    if (viewModel.metadataActions.willFavorite(selectedEntries)) {
                        flags.collectionPickerVisible = true
                    } else {
                        scope.launch { viewModel.metadataActions.toggleFavorite(selectedEntries) }
                    }
                },
                onToggleLock = { scope.launch { viewModel.metadataActions.toggleLock(selectedEntries) } },
                onShowDetails = onShowDetails,
            ),
    )
}

@Composable
private fun SearchDialogsSection(
    flags: SearchUiFlags,
    selectedEntries: List<FileEntry>,
    selectedUris: SnapshotStateList<Uri>,
    viewModel: SearchViewModel,
    onOpenManageTags: () -> Unit,
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val favoriteCollections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (flags.tagPickerVisible) {
        TagPickerDialog(
            tags = tags,
            onApply = { tagIds ->
                val uris = selectedEntries.map { it.uri }
                scope.launch { tagIds.forEach { tagId -> viewModel.metadataActions.applyTag(uris, tagId) } }
                flags.tagPickerVisible = false
                selectedUris.clear()
            },
            onManageTags = {
                flags.tagPickerVisible = false
                onOpenManageTags()
            },
            onDismiss = { flags.tagPickerVisible = false },
        )
    }

    if (flags.detailsVisible) {
        val target = selectedEntries.firstOrNull()
        if (target == null) {
            flags.detailsVisible = false
        } else {
            FileDetailsSheet(entry = target, onDismiss = { flags.detailsVisible = false }, onOpenManageTags = onOpenManageTags)
        }
    }

    if (flags.collectionPickerVisible) {
        FavoriteCollectionPickerDialog(
            collections = favoriteCollections,
            onSelect = { collectionId ->
                scope.launch { viewModel.metadataActions.favoriteInto(selectedEntries, collectionId) }
                flags.collectionPickerVisible = false
            },
            onDismiss = { flags.collectionPickerVisible = false },
        )
    }

    if (flags.addToVaultConfirmVisible) {
        SearchAddToVaultConfirmDialog(
            entries = selectedEntries,
            onConfirm = {
                viewModel.addToVault(selectedEntries)
                selectedUris.clear()
                flags.addToVaultConfirmVisible = false
            },
            onDismiss = { flags.addToVaultConfirmVisible = false },
        )
    }
}

@Composable
private fun SearchAddToVaultConfirmDialog(
    entries: List<FileEntry>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDangerousActionDialog(
        title = stringResource(R.string.add_to_vault_confirm_title),
        message = stringResource(R.string.add_to_vault_confirm_message, entries.size),
        confirmLabel = stringResource(R.string.add_to_vault_confirm_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
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
    onAddToVault: () -> Unit,
    actions: MetadataQuickActions,
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
                SearchOverflowMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    quickActions = actions,
                    onAddToVault = onAddToVault,
                )
            }
        },
    )
}

@Composable
private fun SearchOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    quickActions: MetadataQuickActions,
    onAddToVault: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val onShowDetails = quickActions.onShowDetails
        if (onShowDetails != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.file_details_title)) },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onShowDetails()
                },
            )
        }
        MetadataMenuItems(
            onAddTag = {
                onDismiss()
                quickActions.onAddTag()
            },
            onToggleFavorite = {
                onDismiss()
                quickActions.onToggleFavorite()
            },
            onToggleLock = {
                onDismiss()
                quickActions.onToggleLock()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_add_to_vault)) },
            onClick = {
                onDismiss()
                onAddToVault()
            },
        )
    }
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
