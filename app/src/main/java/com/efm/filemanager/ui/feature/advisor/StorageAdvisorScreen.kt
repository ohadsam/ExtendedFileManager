package com.efm.filemanager.ui.feature.advisor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.efm.filemanager.data.advisor.AdvisorScanProgress
import com.efm.filemanager.domain.model.StorageRecommendation
import com.efm.filemanager.domain.model.StorageRecommendationCategory
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog

/** [StorageAdvisorScreen]'s own transient UI state -- kept off its composable's stack to keep the function short. */
private class AdvisorUiFlags {
    var showDeleteConfirm by mutableStateOf(false)
}

@Composable
fun StorageAdvisorScreen(
    onOpenDrawer: () -> Unit,
    onOpenPreview: () -> Unit,
    viewModel: StorageAdvisorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedKeys = remember { mutableStateListOf<RecommendationKey>() }
    val flags = remember { AdvisorUiFlags() }
    val selectedEntries = uiState.recommendations.filter { selectedKeys.contains(it.key()) }

    Scaffold(
        topBar = {
            StorageAdvisorTopBar(
                onOpenDrawer = onOpenDrawer,
                runState = uiState.runState,
                onScan = viewModel::startScan,
                onCancel = viewModel::cancelScan,
            )
        },
        floatingActionButton = {
            AdvisorFab(selectedEntries = selectedEntries, viewModel = viewModel, flags = flags, selectedKeys = selectedKeys)
        },
    ) { innerPadding ->
        AdvisorBody(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            selectedKeys = selectedKeys,
            onPreview = { recommendation ->
                viewModel.openPreview(uiState.recommendations, recommendation)
                onOpenPreview()
            },
        )
    }

    AdvisorDialogsSection(flags = flags, selectedEntries = selectedEntries, selectedKeys = selectedKeys, viewModel = viewModel)
}

@Composable
private fun AdvisorDialogsSection(
    flags: AdvisorUiFlags,
    selectedEntries: List<StorageRecommendation>,
    selectedKeys: SnapshotStateList<RecommendationKey>,
    viewModel: StorageAdvisorViewModel,
) {
    if (flags.showDeleteConfirm) {
        AdvisorDeleteConfirmDialog(
            entries = selectedEntries,
            onConfirm = {
                viewModel.deleteRecommendations(selectedEntries)
                selectedKeys.clear()
                flags.showDeleteConfirm = false
            },
            onDismiss = { flags.showDeleteConfirm = false },
        )
    }
}

@Composable
private fun AdvisorFab(
    selectedEntries: List<StorageRecommendation>,
    viewModel: StorageAdvisorViewModel,
    flags: AdvisorUiFlags,
    selectedKeys: SnapshotStateList<RecommendationKey>,
) {
    if (selectedEntries.isEmpty()) return
    AdvisorSelectionFabs(
        onDismissClick = {
            viewModel.dismiss(selectedEntries)
            selectedKeys.clear()
        },
        onDeleteClick = { flags.showDeleteConfirm = true },
    )
}

@Composable
private fun AdvisorSelectionFabs(
    onDismissClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        ExtendedFloatingActionButton(
            onClick = onDismissClick,
            icon = { Icon(Icons.Filled.Close, contentDescription = null) },
            text = { Text(stringResource(R.string.storage_advisor_dismiss)) },
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
private fun AdvisorDeleteConfirmDialog(
    entries: List<StorageRecommendation>,
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
private fun AdvisorBody(
    modifier: Modifier = Modifier,
    uiState: StorageAdvisorUiState,
    selectedKeys: SnapshotStateList<RecommendationKey>,
    onPreview: (StorageRecommendation) -> Unit,
) {
    val grouped =
        StorageRecommendationCategory.entries
            .associateWith { category -> uiState.recommendations.filter { it.category == category } }
            .filterValues { it.isNotEmpty() }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.runState == AdvisorScanRunState.RUNNING) {
            AdvisorScanProgressBar(uiState.progress)
        }
        if (grouped.isEmpty()) {
            if (uiState.runState != AdvisorScanRunState.RUNNING) AdvisorEmptyState()
        } else {
            LazyColumn {
                grouped.forEach { (category, recommendations) ->
                    item(key = "header_${category.name}") { CategoryHeader(category, recommendations.size) }
                    items(recommendations, key = { "${it.entry.uri}_${it.category}" }) { recommendation ->
                        RecommendationRow(
                            recommendation = recommendation,
                            isSelected = selectedKeys.contains(recommendation.key()),
                            onClick = { toggleSelection(selectedKeys, recommendation.key()) },
                            onLongClick = { onPreview(recommendation) },
                        )
                    }
                }
            }
        }
    }
}

private fun toggleSelection(
    selectedKeys: SnapshotStateList<RecommendationKey>,
    key: RecommendationKey,
) {
    if (selectedKeys.contains(key)) selectedKeys.remove(key) else selectedKeys.add(key)
}

@Composable
private fun AdvisorScanProgressBar(progress: AdvisorScanProgress?) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.storage_advisor_scanning, progress?.filesScanned ?: 0), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun AdvisorEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.storage_advisor_empty),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}
