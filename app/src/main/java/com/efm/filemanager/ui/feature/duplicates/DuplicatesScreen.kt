package com.efm.filemanager.ui.feature.duplicates

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.data.duplicates.ScanPhase
import com.efm.filemanager.data.duplicates.ScanProgress
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog

@Composable
fun DuplicatesScreen(
    onOpenDrawer: () -> Unit,
    viewModel: DuplicatesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val selectedEntries = uiState.groups.flatMap { it.files }.filter { selectedUris.contains(it.uri) }

    Scaffold(
        topBar = {
            DuplicatesTopBar(
                onOpenDrawer = onOpenDrawer,
                runState = uiState.runState,
                onScan = viewModel::startScan,
                onCancel = viewModel::cancelScan,
            )
        },
        floatingActionButton = {
            if (selectedEntries.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteConfirm = true },
                    icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_delete)) },
                )
            }
        },
    ) { innerPadding ->
        DuplicatesBody(modifier = Modifier.padding(innerPadding), uiState = uiState, selectedUris = selectedUris)
    }

    if (showDeleteConfirm) {
        DuplicateDeleteConfirmDialog(
            entries = selectedEntries,
            onConfirm = {
                viewModel.deleteEntries(selectedEntries)
                selectedUris.removeAll(selectedEntries.map { it.uri })
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun DuplicateDeleteConfirmDialog(
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

@Composable
private fun DuplicatesBody(
    modifier: Modifier = Modifier,
    uiState: DuplicatesUiState,
    selectedUris: SnapshotStateList<Uri>,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.runState == ScanRunState.RUNNING) {
            ScanProgressBar(uiState.progress)
        }
        if (uiState.groups.isEmpty()) {
            if (uiState.runState != ScanRunState.RUNNING) DuplicatesEmptyState()
        } else {
            LazyColumn {
                items(uiState.groups, key = { it.hash }) { group -> DuplicateGroupCard(group = group, selectedUris = selectedUris) }
            }
        }
    }
}

@Composable
private fun ScanProgressBar(progress: ScanProgress?) {
    Column(modifier = Modifier.padding(16.dp)) {
        val labelRes = if (progress?.phase == ScanPhase.VERIFYING) R.string.duplicates_verifying else R.string.duplicates_comparing
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall)
        if (progress != null && progress.total > 0) {
            val fraction = progress.current.toFloat() / progress.total.toFloat()
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun DuplicatesEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.duplicates_empty),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}
