package com.efm.filemanager.ui.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.ui.components.query.labelRes
import com.efm.filemanager.ui.feature.browse.formatFileSize

@Composable
fun StatisticsScreen(
    onOpenDrawer: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { StatisticsTopBar(onOpenDrawer = onOpenDrawer, onRefresh = viewModel::refresh) },
    ) { innerPadding ->
        StatisticsBody(modifier = Modifier.padding(innerPadding), uiState = uiState)
    }
}

@Composable
private fun StatisticsTopBar(
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_statistics)) },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.statistics_refresh))
            }
        },
    )
}

@Composable
private fun StatisticsBody(
    modifier: Modifier,
    uiState: StatisticsUiState,
) {
    if (uiState.isLoading && uiState.storageStats.totalFileCount == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StorageUsedCard(uiState.storageStats) }
        item { FileTypeBreakdownCard(uiState.storageStats.sizeByCategory) }
        item { LargestFilesCard(uiState.storageStats.largestFiles) }
        item { DuplicatesStatCard(uiState.duplicateGroupCount, uiState.reclaimableBytes) }
        item { AdvisorStatCard(uiState.advisorFlaggedCount) }
        item { OperationsStatCard(uiState.totalOperationsCount) }
    }
}

@Composable
private fun StatCard(
    title: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StorageUsedCard(stats: StorageStats) {
    StatCard(title = stringResource(R.string.statistics_storage_used_title)) {
        Text(formatFileSize(stats.totalSize), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.statistics_file_count, stats.totalFileCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileTypeBreakdownCard(sizeByCategory: Map<FileCategory, Long>) {
    StatCard(title = stringResource(R.string.statistics_by_type_title)) {
        if (sizeByCategory.isEmpty()) {
            Text(stringResource(R.string.statistics_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            sizeByCategory.entries.sortedByDescending { it.value }.forEach { (category, size) ->
                StatRow(label = stringResource(category.labelRes()), value = formatFileSize(size))
            }
        }
    }
}

@Composable
private fun LargestFilesCard(largestFiles: List<FileEntry>) {
    StatCard(title = stringResource(R.string.statistics_largest_files_title)) {
        if (largestFiles.isEmpty()) {
            Text(stringResource(R.string.statistics_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            largestFiles.forEach { file -> StatRow(label = file.name, value = formatFileSize(file.size)) }
        }
    }
}

@Composable
private fun DuplicatesStatCard(
    groupCount: Int,
    reclaimableBytes: Long,
) {
    StatCard(title = stringResource(R.string.statistics_duplicates_title)) {
        StatRow(label = stringResource(R.string.statistics_duplicate_groups), value = groupCount.toString())
        StatRow(label = stringResource(R.string.statistics_reclaimable_space), value = formatFileSize(reclaimableBytes))
    }
}

@Composable
private fun AdvisorStatCard(flaggedCount: Int) {
    StatCard(title = stringResource(R.string.statistics_advisor_title)) {
        Text(stringResource(R.string.statistics_advisor_flagged_count, flaggedCount), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun OperationsStatCard(totalCount: Int) {
    StatCard(title = stringResource(R.string.statistics_operations_title)) {
        Text(stringResource(R.string.statistics_operations_count, totalCount), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
