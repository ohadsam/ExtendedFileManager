package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.query.groupResult
import com.efm.filemanager.ui.components.GroupedFileList

@Composable
fun BrowseScreen(
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
    archiveViewModel: ArchiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerState by viewModel.pickerState.collectAsStateWithLifecycle()
    val treePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) viewModel.onTreeGranted(uri)
        }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val dialogState = remember { BrowseScreenDialogState() }
    val selectedEntries = uiState.files.filter { selectedUris.contains(it.uri) }
    val snackbarHostState = remember { SnackbarHostState() }
    val selectionActions = buildSelectionActions(selectedUris, selectedEntries, viewModel) { dialogState.dialog = it }
    val archiveActions = buildArchiveBarActions(selectedEntries) { dialogState.archiveRequest = it }
    val selectionBarState = buildSelectionBarState(selectedUris, selectedEntries, selectionActions, archiveActions)

    BrowseSnackbarEffect(viewModel = viewModel, snackbarHostState = snackbarHostState)

    Scaffold(
        topBar = {
            BrowseTopBar(
                state = BrowseTopBarState(uiState.breadcrumbs, selectionBarState, uiState.querySpec),
                actions =
                    BrowseTopBarActions(
                        onOpenDrawer = onOpenDrawer,
                        onNavigateToBreadcrumb = viewModel::navigateToBreadcrumb,
                        onQuerySpecChanged = viewModel::updateQuerySpec,
                        onOpenSearch = onOpenSearch,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            BrowseFab(
                visible = uiState.hasAccess && selectedUris.isEmpty(),
                onClick = { dialogState.dialog = BrowseDialog.CREATE },
            )
        },
    ) { innerPadding ->
        BrowseBody(
            uiState = uiState,
            selectedUris = selectedUris,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
            onGrantClick = { treePickerLauncher.launch(null) },
        )
    }

    BrowseScreenDialogs(
        dialogState = dialogState,
        viewModel = viewModel,
        archiveViewModel = archiveViewModel,
        context = DialogsContext(selectedEntries, pickerState, uiState.breadcrumbs.lastOrNull()?.uri),
        onSelectionCleared = { selectedUris.clear() },
    )
}

@Composable
private fun BrowseFab(
    visible: Boolean,
    onClick: () -> Unit,
) {
    if (visible) {
        FloatingActionButton(onClick = onClick) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_entry_title))
        }
    }
}

@Composable
private fun BrowseScreenDialogs(
    dialogState: BrowseScreenDialogState,
    viewModel: BrowseViewModel,
    archiveViewModel: ArchiveViewModel,
    context: DialogsContext,
    onSelectionCleared: () -> Unit,
) {
    BrowseDialogs(
        dialog = dialogState.dialog,
        onFinished = {
            dialogState.dialog = null
            onSelectionCleared()
        },
        viewModel = viewModel,
        selectedEntries = context.selectedEntries,
        pickerState = context.pickerState,
    )

    ArchiveDialogsSection(
        request = dialogState.archiveRequest,
        parentUri = context.parentUri,
        archiveViewModel = archiveViewModel,
        selectedEntries = context.selectedEntries,
        onFinished = {
            dialogState.archiveRequest = null
            onSelectionCleared()
        },
    )
}

private fun onFileEntryTapped(
    entry: FileEntry,
    selectedUris: SnapshotStateList<Uri>,
    viewModel: BrowseViewModel,
) {
    if (selectedUris.isNotEmpty()) {
        toggleSelection(selectedUris, entry.uri)
    } else if (entry.isDirectory) {
        viewModel.openEntry(entry)
    }
}

private fun toggleSelection(
    selected: SnapshotStateList<Uri>,
    uri: Uri,
) {
    if (selected.contains(uri)) selected.remove(uri) else selected.add(uri)
}

@Composable
private fun BrowseBody(
    uiState: BrowseUiState,
    selectedUris: SnapshotStateList<Uri>,
    viewModel: BrowseViewModel,
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit,
) {
    val groups = remember(uiState.files, uiState.querySpec) { uiState.querySpec.groupResult(uiState.files) }
    when {
        !uiState.hasAccess -> GrantAccessEmptyState(modifier = modifier, onGrantClick = onGrantClick)
        uiState.isLoading && uiState.files.isEmpty() -> LoadingState(modifier)
        uiState.files.isEmpty() -> EmptyFolderState(modifier)
        groups.sumOf { it.files.size } == 0 -> NoMatchesState(modifier)
        else ->
            GroupedFileList(
                modifier = modifier,
                groups = groups,
                selectedUris = selectedUris,
                onEntryClick = { entry -> onFileEntryTapped(entry, selectedUris, viewModel) },
                onEntryLongClick = { entry -> toggleSelection(selectedUris, entry.uri) },
            )
    }
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

@Composable
private fun NoMatchesState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.no_matches))
    }
}
