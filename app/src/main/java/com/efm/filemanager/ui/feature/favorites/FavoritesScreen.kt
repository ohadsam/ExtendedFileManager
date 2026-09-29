package com.efm.filemanager.ui.feature.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.previewType
import com.efm.filemanager.ui.components.FileRow

@Composable
fun FavoritesScreen(
    onOpenDrawer: () -> Unit,
    onOpenPreview: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pathStack by remember { mutableStateOf(listOf<FavoriteCollection>()) }
    var dialog by remember { mutableStateOf<FavoritesDialog?>(null) }
    val currentId = pathStack.lastOrNull()?.id
    val childCollections = uiState.collections.filter { it.parentId == currentId }
    val childEntries = uiState.entries.filter { it.favoriteCollectionId == currentId }

    val actions =
        FavoritesRowActions(
            onOpenCollection = { collection -> pathStack = pathStack + collection },
            onMoveCollection = viewModel::moveCollection,
            onRenameCollection = { collection -> dialog = FavoritesDialog.Rename(collection) },
            onDeleteCollection = { collection -> dialog = FavoritesDialog.Delete(collection) },
            onEntryClick = { entry ->
                if (entry.previewType() != PreviewType.NONE) {
                    viewModel.openPreview(childEntries, entry)
                    onOpenPreview()
                }
            },
            onEntryLongClick = { entry -> viewModel.removeFavorite(entry) },
        )

    Scaffold(
        topBar = {
            FavoritesTopBar(
                title = pathStack.lastOrNull()?.name ?: stringResource(R.string.nav_favorites),
                atRoot = pathStack.isEmpty(),
                onOpenDrawer = onOpenDrawer,
                onNavigateBack = { pathStack = pathStack.dropLast(1) },
                onCreateCollection = { dialog = FavoritesDialog.CreateCollection(currentId) },
            )
        },
    ) { innerPadding ->
        FavoritesBody(
            modifier = Modifier.padding(innerPadding),
            collections = childCollections,
            entries = childEntries,
            actions = actions,
        )
    }

    FavoritesDialogHost(dialog = dialog, viewModel = viewModel, onDismiss = { dialog = null })
}

@Composable
private fun FavoritesTopBar(
    title: String,
    atRoot: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit,
    onCreateCollection: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            if (atRoot) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
                }
            } else {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                }
            }
        },
        title = { Text(title) },
        actions = {
            IconButton(onClick = onCreateCollection) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.favorites_new_collection))
            }
        },
    )
}

@Composable
private fun FavoritesBody(
    modifier: Modifier = Modifier,
    collections: List<FavoriteCollection>,
    entries: List<FileEntry>,
    actions: FavoritesRowActions,
) {
    if (collections.isEmpty() && entries.isEmpty()) {
        FavoritesEmptyState(modifier)
        return
    }
    LazyColumn(modifier = modifier) {
        items(collections, key = { "collection_${it.id}" }) { collection -> CollectionRow(collection, actions) }
        items(entries, key = { it.uri.toString() }) { entry ->
            FileRow(
                entry = entry,
                isSelected = false,
                onClick = { actions.onEntryClick(entry) },
                onLongClick = { actions.onEntryLongClick(entry) },
            )
        }
    }
}

@Composable
private fun FavoritesEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.favorites_empty), modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun CollectionRow(
    collection: FavoriteCollection,
    actions: FavoritesRowActions,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { actions.onOpenCollection(collection) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
        Text(text = collection.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
            }
            CollectionMenu(expanded = menuExpanded, collection = collection, actions = actions, onDismiss = { menuExpanded = false })
        }
    }
}

@Composable
private fun CollectionMenu(
    expanded: Boolean,
    collection: FavoriteCollection,
    actions: FavoritesRowActions,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.favorites_move_up)) },
            onClick = {
                onDismiss()
                actions.onMoveCollection(collection, MoveDirection.UP)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.favorites_move_down)) },
            onClick = {
                onDismiss()
                actions.onMoveCollection(collection, MoveDirection.DOWN)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.rename_title)) },
            onClick = {
                onDismiss()
                actions.onRenameCollection(collection)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete)) },
            onClick = {
                onDismiss()
                actions.onDeleteCollection(collection)
            },
        )
    }
}
