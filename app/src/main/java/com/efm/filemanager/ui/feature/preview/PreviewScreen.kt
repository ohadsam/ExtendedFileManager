package com.efm.filemanager.ui.feature.preview

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
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
import com.efm.filemanager.ui.components.MetadataQuickActionsMenu
import com.efm.filemanager.ui.components.TagPickerDialog
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
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var tagPickerVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentEntry = session.entries.getOrNull(pagerState.currentPage)
    val currentUri = currentEntry?.uri
    val currentSelected = currentUri != null && selectedUris.contains(currentUri)

    // "Select as you go": once a selection is active, swiping to a new page adds it too,
    // instead of only ever acting on whichever file happens to be on screen right now.
    LaunchedEffect(pagerState.currentPage) {
        val entry = currentEntry
        if (entry != null && selectedUris.isNotEmpty() && entry.uri !in selectedUris) {
            selectedUris.add(entry.uri)
        }
    }

    Scaffold(
        topBar = {
            if (selectedUris.isEmpty()) {
                PreviewNormalTopBar(
                    title = currentEntry?.name.orEmpty(),
                    onNavigateBack = onNavigateBack,
                    onStartSelection = { currentEntry?.let { selectedUris.add(it.uri) } },
                )
            } else {
                val actions =
                    PreviewSelectionActions(
                        onClose = { selectedUris.clear() },
                        onToggleCurrent = { currentEntry?.let { toggleSelection(selectedUris, it.uri) } },
                        onAddTag = { tagPickerVisible = true },
                        onToggleFavorite = {
                            val entries = session.entries.filter { it.uri in selectedUris }
                            scope.launch { viewModel.metadataActions.toggleFavorite(entries) }
                        },
                        onToggleLock = {
                            val entries = session.entries.filter { it.uri in selectedUris }
                            scope.launch { viewModel.metadataActions.toggleLock(entries) }
                        },
                    )
                PreviewSelectionTopBar(
                    count = selectedUris.size,
                    currentSelected = currentSelected,
                    actions = actions,
                )
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { page -> PreviewPage(entry = session.entries[page]) }
    }

    if (tagPickerVisible) {
        TagPickerDialog(
            tags = tags,
            onApply = { tagIds ->
                val uris = selectedUris.toList()
                scope.launch { tagIds.forEach { tagId -> viewModel.metadataActions.applyTag(uris, tagId) } }
                tagPickerVisible = false
            },
            onManageTags = {
                tagPickerVisible = false
                onOpenManageTags()
            },
            onDismiss = { tagPickerVisible = false },
        )
    }
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
                    onAddTag = actions.onAddTag,
                    onToggleFavorite = actions.onToggleFavorite,
                    onToggleLock = actions.onToggleLock,
                )
            }
        },
    )
}
