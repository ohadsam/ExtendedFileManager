package com.efm.filemanager.ui.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.ui.components.containerColor

@Composable
fun ManageTagsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageTagsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<ManageTagsDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_tags_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = ManageTagsDialog.Create }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.manage_tags_create))
            }
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            items(uiState.tags, key = { it.id }) { tag ->
                TagRow(
                    tag = tag,
                    onTogglePin = { viewModel.togglePinned(tag) },
                    onEdit = { dialog = ManageTagsDialog.Edit(tag) },
                    onMerge = { dialog = ManageTagsDialog.Merge(tag) },
                    onDelete = { dialog = ManageTagsDialog.Delete(tag) },
                )
            }
        }
    }

    ManageTagsDialogHost(
        dialog = dialog,
        allTags = uiState.tags,
        viewModel = viewModel,
        onDismiss = { dialog = null },
    )
}

@Composable
private fun TagRow(
    tag: FileTag,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(tag.color.containerColor()))
        Text(text = tag.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp).weight(1f))
        IconButton(onClick = onTogglePin) {
            Icon(
                imageVector = if (tag.pinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = stringResource(R.string.manage_tags_pin),
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manage_tags_edit)) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manage_tags_merge)) },
                    onClick = {
                        menuExpanded = false
                        onMerge()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}
