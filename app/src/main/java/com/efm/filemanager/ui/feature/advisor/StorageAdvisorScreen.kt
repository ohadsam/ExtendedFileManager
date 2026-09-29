package com.efm.filemanager.ui.feature.advisor

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
private class AdvisorScreenState {
    var tab by mutableStateOf(AdvisorTab.RECOMMENDATIONS)
    val selectedKeys = mutableStateListOf<RecommendationKey>()
    val selectedStagedUris = mutableStateListOf<Uri>()
    var showDeleteConfirm by mutableStateOf(false)
    var showStagedDeleteConfirm by mutableStateOf(false)
}

@Composable
fun StorageAdvisorScreen(
    onOpenDrawer: () -> Unit,
    onOpenPreview: () -> Unit,
    viewModel: StorageAdvisorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = remember { AdvisorScreenState() }

    Scaffold(
        topBar = {
            StorageAdvisorTopBar(
                onOpenDrawer = onOpenDrawer,
                runState = uiState.runState,
                onScan = viewModel::startScan,
                onCancel = viewModel::cancelScan,
            )
        },
        floatingActionButton = { AdvisorScreenFab(state = state, uiState = uiState, viewModel = viewModel) },
    ) { innerPadding ->
        AdvisorScreenBody(
            modifier = Modifier.padding(innerPadding),
            state = state,
            uiState = uiState,
            viewModel = viewModel,
            onOpenPreview = onOpenPreview,
        )
    }

    AdvisorScreenDialogs(state = state, uiState = uiState, viewModel = viewModel)
}

@Composable
private fun AdvisorScreenFab(
    state: AdvisorScreenState,
    uiState: StorageAdvisorUiState,
    viewModel: StorageAdvisorViewModel,
) {
    if (state.tab == AdvisorTab.RECOMMENDATIONS) {
        val selected = uiState.recommendations.filter { state.selectedKeys.contains(it.key()) }
        AdvisorFab(selectedEntries = selected, viewModel = viewModel, state = state)
    } else {
        val selected = uiState.stagedEntries.filter { state.selectedStagedUris.contains(it.uri) }
        StagedFab(
            selectedEntries = selected,
            onUnstageClick = {
                viewModel.unstage(selected)
                state.selectedStagedUris.clear()
            },
            onDeleteClick = { state.showStagedDeleteConfirm = true },
        )
    }
}

@Composable
private fun AdvisorScreenBody(
    modifier: Modifier,
    state: AdvisorScreenState,
    uiState: StorageAdvisorUiState,
    viewModel: StorageAdvisorViewModel,
    onOpenPreview: () -> Unit,
) {
    Column(modifier = modifier) {
        AdvisorTabRow(tab = state.tab, stagedCount = uiState.stagedEntries.size, onTabSelected = { state.tab = it })
        if (state.tab == AdvisorTab.RECOMMENDATIONS) {
            AdvisorBody(
                uiState = uiState,
                selectedKeys = state.selectedKeys,
                onPreview = { recommendation ->
                    viewModel.openPreview(uiState.recommendations, recommendation)
                    onOpenPreview()
                },
            )
        } else {
            StagedBody(
                entries = uiState.stagedEntries,
                selectedUris = state.selectedStagedUris,
                onPreview = { entry ->
                    viewModel.openStagedPreview(uiState.stagedEntries, entry)
                    onOpenPreview()
                },
            )
        }
    }
}

@Composable
private fun AdvisorTabRow(
    tab: AdvisorTab,
    stagedCount: Int,
    onTabSelected: (AdvisorTab) -> Unit,
) {
    TabRow(selectedTabIndex = tab.ordinal) {
        Tab(
            selected = tab == AdvisorTab.RECOMMENDATIONS,
            onClick = { onTabSelected(AdvisorTab.RECOMMENDATIONS) },
            text = { Text(stringResource(R.string.storage_advisor_tab_recommendations)) },
        )
        Tab(
            selected = tab == AdvisorTab.STAGED,
            onClick = { onTabSelected(AdvisorTab.STAGED) },
            text = { Text(stringResource(R.string.storage_advisor_tab_staged, stagedCount)) },
        )
    }
}

@Composable
private fun AdvisorScreenDialogs(
    state: AdvisorScreenState,
    uiState: StorageAdvisorUiState,
    viewModel: StorageAdvisorViewModel,
) {
    if (state.showDeleteConfirm) {
        val selected = uiState.recommendations.filter { state.selectedKeys.contains(it.key()) }
        AdvisorDeleteConfirmDialog(
            entries = selected,
            onConfirm = {
                viewModel.deleteRecommendations(selected)
                state.selectedKeys.clear()
                state.showDeleteConfirm = false
            },
            onDismiss = { state.showDeleteConfirm = false },
        )
    }
    if (state.showStagedDeleteConfirm) {
        val selected = uiState.stagedEntries.filter { state.selectedStagedUris.contains(it.uri) }
        StagedDeleteConfirmDialog(
            entries = selected,
            onConfirm = {
                viewModel.deleteStagedEntries(selected)
                state.selectedStagedUris.clear()
                state.showStagedDeleteConfirm = false
            },
            onDismiss = { state.showStagedDeleteConfirm = false },
        )
    }
}

@Composable
private fun AdvisorFab(
    selectedEntries: List<StorageRecommendation>,
    viewModel: StorageAdvisorViewModel,
    state: AdvisorScreenState,
) {
    if (selectedEntries.isEmpty()) return
    AdvisorSelectionFabs(
        onStageClick = {
            viewModel.stageForLater(selectedEntries.map { it.entry })
            state.selectedKeys.clear()
        },
        onDismissClick = {
            viewModel.dismiss(selectedEntries)
            state.selectedKeys.clear()
        },
        onDeleteClick = { state.showDeleteConfirm = true },
    )
}

@Composable
private fun AdvisorSelectionFabs(
    onStageClick: () -> Unit,
    onDismissClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            FloatingActionButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.selection_more_actions))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.storage_advisor_stage_for_later)) },
                    onClick = {
                        menuExpanded = false
                        onStageClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.storage_advisor_dismiss)) },
                    onClick = {
                        menuExpanded = false
                        onDismissClick()
                    },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = onDeleteClick,
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            text = { Text(stringResource(R.string.action_delete)) },
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
