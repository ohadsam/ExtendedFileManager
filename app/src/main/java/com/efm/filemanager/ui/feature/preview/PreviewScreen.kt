package com.efm.filemanager.ui.feature.preview

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.ui.components.FavoriteCollectionPickerDialog
import com.efm.filemanager.ui.components.MetadataQuickActions
import com.efm.filemanager.ui.components.MetadataQuickActionsMenu
import com.efm.filemanager.ui.components.TagPickerDialog
import com.efm.filemanager.ui.feature.filedetails.FileDetailsSheet
import kotlinx.coroutines.launch

@Composable
fun PreviewScreen(
    onNavigateBack: () -> Unit,
    onOpenManageTags: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val current = session
    if (current == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
    } else {
        PreviewContent(session = current, onNavigateBack = onNavigateBack, onOpenManageTags = onOpenManageTags, viewModel = viewModel)
    }
}

@Composable
private fun PreviewContent(
    session: PreviewSession,
    onNavigateBack: () -> Unit,
    onOpenManageTags: () -> Unit,
    viewModel: PreviewViewModel,
) {
    val pagerState = rememberPagerState(initialPage = session.startIndex) { session.entries.size }
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val favoriteCollections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var tagPickerVisible by remember { mutableStateOf(false) }
    var detailsVisible by remember { mutableStateOf(false) }
    var collectionPickerVisible by remember { mutableStateOf(false) }

    // "Select as you go": once a selection is active, swiping to a new page adds it too,
    // instead of only ever acting on whichever file happens to be on screen right now.
    LaunchedEffect(pagerState.currentPage) {
        val entry = session.entries.getOrNull(pagerState.currentPage)
        if (entry != null && selectedUris.isNotEmpty() && entry.uri !in selectedUris) {
            selectedUris.add(entry.uri)
        }
    }

    Scaffold(
        topBar = {
            PreviewTopBar(
                session = session,
                pagerState = pagerState,
                selectedUris = selectedUris,
                callbacks =
                    PreviewTopBarCallbacks(
                        onNavigateBack = onNavigateBack,
                        onAddTag = { tagPickerVisible = true },
                        onShowDetails = { detailsVisible = true },
                        onNeedsFavoriteCollection = { collectionPickerVisible = true },
                    ),
                viewModel = viewModel,
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { page ->
            val entry = session.entries[page]
            PreviewPage(
                entry = entry,
                onLongPress = { if (selectedUris.isEmpty()) selectedUris.add(entry.uri) },
            )
        }
    }

    if (tagPickerVisible) {
        PreviewTagPickerSheet(
            tags = tags,
            selectedUris = selectedUris,
            viewModel = viewModel,
            onOpenManageTags = onOpenManageTags,
            onDismiss = { tagPickerVisible = false },
        )
    }

    if (detailsVisible) {
        val target = session.entries.firstOrNull { it.uri == selectedUris.firstOrNull() }
        if (target == null) {
            detailsVisible = false
        } else {
            FileDetailsSheet(entry = target, onDismiss = { detailsVisible = false }, onOpenManageTags = onOpenManageTags)
        }
    }

    if (collectionPickerVisible) {
        PreviewCollectionPicker(
            favoriteCollections = favoriteCollections,
            entries = session.entries.filter { it.uri in selectedUris },
            viewModel = viewModel,
            onDismiss = { collectionPickerVisible = false },
        )
    }
}

@Composable
private fun PreviewCollectionPicker(
    favoriteCollections: List<FavoriteCollection>,
    entries: List<FileEntry>,
    viewModel: PreviewViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    FavoriteCollectionPickerDialog(
        collections = favoriteCollections,
        onSelect = { collectionId ->
            scope.launch { viewModel.metadataActions.favoriteInto(entries, collectionId) }
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

private data class PreviewTopBarCallbacks(
    val onNavigateBack: () -> Unit,
    val onAddTag: () -> Unit,
    val onShowDetails: () -> Unit,
    val onNeedsFavoriteCollection: () -> Unit,
)

@Composable
private fun PreviewTopBar(
    session: PreviewSession,
    pagerState: PagerState,
    selectedUris: SnapshotStateList<Uri>,
    callbacks: PreviewTopBarCallbacks,
    viewModel: PreviewViewModel,
) {
    val scope = rememberCoroutineScope()
    val currentEntry = session.entries.getOrNull(pagerState.currentPage)
    val currentUri = currentEntry?.uri
    val currentSelected = currentUri != null && selectedUris.contains(currentUri)

    if (selectedUris.isEmpty()) {
        PreviewNormalTopBar(
            title = currentEntry?.name.orEmpty(),
            onNavigateBack = callbacks.onNavigateBack,
            onStartSelection = { currentEntry?.let { selectedUris.add(it.uri) } },
        )
    } else {
        val actions =
            PreviewSelectionActions(
                onClose = { selectedUris.clear() },
                onToggleCurrent = { currentEntry?.let { toggleSelection(selectedUris, it.uri) } },
                onAddTag = callbacks.onAddTag,
                onToggleFavorite = {
                    val entries = session.entries.filter { it.uri in selectedUris }
                    if (viewModel.metadataActions.willFavorite(entries)) {
                        callbacks.onNeedsFavoriteCollection()
                    } else {
                        scope.launch { viewModel.metadataActions.toggleFavorite(entries) }
                    }
                },
                onToggleLock = {
                    val entries = session.entries.filter { it.uri in selectedUris }
                    scope.launch { viewModel.metadataActions.toggleLock(entries) }
                },
                onShowDetails = if (selectedUris.size == 1) callbacks.onShowDetails else null,
            )
        PreviewSelectionTopBar(
            count = selectedUris.size,
            currentSelected = currentSelected,
            actions = actions,
        )
    }
}

@Composable
private fun PreviewTagPickerSheet(
    tags: List<FileTag>,
    selectedUris: List<Uri>,
    viewModel: PreviewViewModel,
    onOpenManageTags: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    TagPickerDialog(
        tags = tags,
        onApply = { tagIds ->
            val uris = selectedUris.toList()
            scope.launch { tagIds.forEach { tagId -> viewModel.metadataActions.applyTag(uris, tagId) } }
            onDismiss()
        },
        onManageTags = {
            onDismiss()
            onOpenManageTags()
        },
        onDismiss = onDismiss,
    )
}

private fun toggleSelection(
    selected: SnapshotStateList<Uri>,
    uri: Uri,
) {
    if (selected.contains(uri)) selected.remove(uri) else selected.add(uri)
}

@Composable
private fun PreviewNormalTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onStartSelection: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
        },
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            IconButton(onClick = onStartSelection) {
                Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = stringResource(R.string.preview_select))
            }
        },
    )
}

@Composable
private fun PreviewSelectionTopBar(
    count: Int,
    currentSelected: Boolean,
    actions: PreviewSelectionActions,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = actions.onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.selection_clear))
            }
        },
        title = { Text(stringResource(R.string.selection_count, count)) },
        actions = {
            IconButton(onClick = actions.onToggleCurrent) {
                val icon = if (currentSelected) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked
                Icon(icon, contentDescription = stringResource(R.string.preview_select))
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
                }
                MetadataQuickActionsMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    actions =
                        MetadataQuickActions(
                            onAddTag = actions.onAddTag,
                            onToggleFavorite = actions.onToggleFavorite,
                            onToggleLock = actions.onToggleLock,
                            onShowDetails = actions.onShowDetails,
                        ),
                )
            }
        },
    )
}
