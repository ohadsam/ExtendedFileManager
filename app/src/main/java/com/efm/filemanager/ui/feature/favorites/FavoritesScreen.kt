package com.efm.filemanager.ui.feature.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.previewType
import com.efm.filemanager.ui.components.FileRow
import com.efm.filemanager.ui.feature.filedetails.FileDetailsSheet

private val COLLECTION_ROW_HEIGHT = 48.dp

/** [FavoritesScreen]'s own transient UI state -- kept off its composable's stack to keep the function short. */
private class FavoritesScreenState {
    var pathStack by mutableStateOf(listOf<FavoriteCollection>())
    var dialog by mutableStateOf<FavoritesDialog?>(null)
    var detailsTarget by mutableStateOf<FileEntry?>(null)
}

@Composable
fun FavoritesScreen(
    onOpenDrawer: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenManageTags: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = remember { FavoritesScreenState() }
    val currentId = state.pathStack.lastOrNull()?.id
    val childCollections = uiState.collections.filter { it.parentId == currentId }
    val childEntries = uiState.entries.filter { it.favoriteCollectionId == currentId }
    val actions = buildFavoritesActions(state, childEntries, viewModel, onOpenPreview)

    Scaffold(
        topBar = {
            FavoritesTopBar(
                title = state.pathStack.lastOrNull()?.name ?: stringResource(R.string.nav_favorites),
                atRoot = state.pathStack.isEmpty(),
                onOpenDrawer = onOpenDrawer,
                onNavigateBack = { state.pathStack = state.pathStack.dropLast(1) },
                onCreateCollection = { state.dialog = FavoritesDialog.CreateCollection(currentId) },
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

    FavoritesDialogsSection(state = state, viewModel = viewModel, onOpenManageTags = onOpenManageTags)
}

private fun buildFavoritesActions(
    state: FavoritesScreenState,
    childEntries: List<FileEntry>,
    viewModel: FavoritesViewModel,
    onOpenPreview: () -> Unit,
): FavoritesRowActions =
    FavoritesRowActions(
        onOpenCollection = { collection -> state.pathStack = state.pathStack + collection },
        onMoveCollection = viewModel::moveCollection,
        onRenameCollection = { collection -> state.dialog = FavoritesDialog.Rename(collection) },
        onDeleteCollection = { collection -> state.dialog = FavoritesDialog.Delete(collection) },
        onEntryClick = { entry ->
            if (entry.previewType() != PreviewType.NONE) {
                viewModel.openPreview(childEntries, entry)
                onOpenPreview()
            }
        },
        onEntryLongClick = { entry -> viewModel.removeFavorite(entry) },
        onShowDetails = { entry -> state.detailsTarget = entry },
    )

@Composable
private fun FavoritesDialogsSection(
    state: FavoritesScreenState,
    viewModel: FavoritesViewModel,
    onOpenManageTags: () -> Unit,
) {
    FavoritesDialogHost(dialog = state.dialog, viewModel = viewModel, onDismiss = { state.dialog = null })

    state.detailsTarget?.let { target ->
        FileDetailsSheet(entry = target, onDismiss = { state.detailsTarget = null }, onOpenManageTags = onOpenManageTags)
    }
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

/**
 * A collection being dragged by its handle -- [dragOffsetY] accumulates raw drag distance and
 * every full [COLLECTION_ROW_HEIGHT] crossed triggers one adjacent swap via the same
 * [FavoritesRowActions.onMoveCollection] the menu's move-up/down items already use, so dragging
 * is a real, continuous reorder gesture built on already-exercised, tested logic rather than a
 * from-scratch reimplementation.
 */
private class CollectionDragState {
    var draggingId by mutableStateOf<Long?>(null)
    var offsetY by mutableFloatStateOf(0f)
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
    val dragState = remember { CollectionDragState() }
    val rowHeightPx = with(LocalDensity.current) { COLLECTION_ROW_HEIGHT.toPx() }

    LazyColumn(modifier = modifier) {
        itemsIndexed(collections, key = { _, collection -> "collection_${collection.id}" }) { index, collection ->
            val drag =
                CollectionRowDrag(
                    isDragging = dragState.draggingId == collection.id,
                    offsetY = if (dragState.draggingId == collection.id) dragState.offsetY else 0f,
                    onDrag = { delta ->
                        dragState.draggingId = collection.id
                        dragState.offsetY += delta
                        onDragCrossedRow(dragState, rowHeightPx, index, collections, actions)
                    },
                    onDragEnd = {
                        dragState.draggingId = null
                        dragState.offsetY = 0f
                    },
                )
            CollectionRow(collection = collection, actions = actions, drag = drag)
        }
        items(entries, key = { it.uri.toString() }) { entry ->
            FavoritesEntryRow(entry = entry, actions = actions)
        }
    }
}

@Composable
private fun FavoritesEntryRow(
    entry: FileEntry,
    actions: FavoritesRowActions,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            FileRow(
                entry = entry,
                isSelected = false,
                onClick = { actions.onEntryClick(entry) },
                onLongClick = { actions.onEntryLongClick(entry) },
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.file_details_title)) },
                    onClick = {
                        menuExpanded = false
                        actions.onShowDetails(entry)
                    },
                )
            }
        }
    }
}

private fun onDragCrossedRow(
    dragState: CollectionDragState,
    rowHeightPx: Float,
    index: Int,
    collections: List<FavoriteCollection>,
    actions: FavoritesRowActions,
) {
    if (dragState.offsetY > rowHeightPx && index < collections.lastIndex) {
        actions.onMoveCollection(collections[index], MoveDirection.DOWN)
        dragState.offsetY -= rowHeightPx
    } else if (dragState.offsetY < -rowHeightPx && index > 0) {
        actions.onMoveCollection(collections[index], MoveDirection.UP)
        dragState.offsetY += rowHeightPx
    }
}

@Composable
private fun FavoritesEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.favorites_empty), modifier = Modifier.padding(24.dp))
    }
}

private data class CollectionRowDrag(
    val isDragging: Boolean,
    val offsetY: Float,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit,
)

@Composable
private fun CollectionRow(
    collection: FavoriteCollection,
    actions: FavoritesRowActions,
    drag: CollectionRowDrag,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = drag.offsetY }
                .zIndex(if (drag.isDragging) 1f else 0f)
                .clickable { actions.onOpenCollection(collection) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        DragHandleIcon(onDrag = drag.onDrag, onDragEnd = drag.onDragEnd)
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

/**
 * [onDrag]/[onDragEnd] arrive fresh every recomposition (each one closes over this row's
 * current index and the current collections list), but a reorder swap reuses this same row in
 * place rather than recreating it, and [pointerInput]'s own gesture coroutine -- keyed so it
 * survives that reuse -- would otherwise keep running with only the very first composition's
 * callbacks. Routing both through [rememberUpdatedState] keeps the running coroutine reading
 * the latest callbacks instead of a stale closure from before the row's position last changed.
 */
@Composable
private fun DragHandleIcon(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    Icon(
        imageVector = Icons.Filled.DragHandle,
        contentDescription = stringResource(R.string.favorites_reorder_handle),
        modifier =
            Modifier
                .padding(end = 12.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragEnd() },
                    ) { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.y)
                    }
                },
    )
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
