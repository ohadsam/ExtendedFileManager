package com.efm.filemanager.ui.feature.advisor

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog
import com.efm.filemanager.ui.components.FileRow

@Composable
internal fun StagedBody(
    modifier: Modifier = Modifier,
    entries: List<FileEntry>,
    selectedUris: SnapshotStateList<Uri>,
    onPreview: (FileEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        StagedEmptyState(modifier)
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(entries, key = { it.uri.toString() }) { entry ->
            FileRow(
                entry = entry,
                isSelected = selectedUris.contains(entry.uri),
                onClick = { toggleStagedSelection(selectedUris, entry.uri) },
                onLongClick = { onPreview(entry) },
            )
        }
    }
}

private fun toggleStagedSelection(
    selectedUris: SnapshotStateList<Uri>,
    uri: Uri,
) {
    if (selectedUris.contains(uri)) selectedUris.remove(uri) else selectedUris.add(uri)
}

@Composable
private fun StagedEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.storage_advisor_staged_empty),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
internal fun StagedFab(
    selectedEntries: List<FileEntry>,
    onUnstageClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    if (selectedEntries.isEmpty()) return
    Column(horizontalAlignment = Alignment.End) {
        ExtendedFloatingActionButton(
            onClick = onUnstageClick,
            icon = { Icon(Icons.Filled.RestorePage, contentDescription = null) },
            text = { Text(stringResource(R.string.storage_advisor_unstage)) },
        )
        ExtendedFloatingActionButton(
            onClick = onDeleteClick,
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            text = { Text(stringResource(R.string.action_delete)) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
internal fun StagedDeleteConfirmDialog(
    entries: List<FileEntry>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDangerousActionDialog(
        title = stringResource(R.string.delete_confirm_title),
        message = stringResource(R.string.delete_confirm_message, entries.size),
        confirmLabel = stringResource(R.string.delete_confirm_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
