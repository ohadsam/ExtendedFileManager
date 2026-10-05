package com.efm.filemanager.ui.feature.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.ui.feature.browse.formatFileSize
import com.efm.filemanager.ui.feature.statistics.DuplicatesSummary

@Composable
fun DashboardScreen(
    onOpenDrawer: () -> Unit,
    navActions: DashboardNavActions,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { DashboardTopBar(onOpenDrawer = onOpenDrawer, onOpenSearch = navActions.onOpenSearch) },
    ) { innerPadding ->
        DashboardBody(modifier = Modifier.padding(innerPadding), uiState = uiState, navActions = navActions)
    }
}

@Composable
private fun DashboardTopBar(
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_dashboard)) },
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_icon))
            }
        },
    )
}

@Composable
private fun DashboardBody(
    modifier: Modifier,
    uiState: DashboardUiState,
    navActions: DashboardNavActions,
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
        item { StorageSummaryCard(uiState.storageStats, onClick = navActions.onOpenStatistics) }
        item { DuplicatesCard(uiState.duplicatesSummary, onClick = navActions.onOpenDuplicates) }
        item { InsightsCard(uiState.advisorFlaggedCount, onClick = navActions.onOpenInsights) }
        item { FavoritesCard(uiState.favoriteEntries, onClick = navActions.onOpenFavorites) }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun StorageSummaryCard(
    stats: StorageStats,
    onClick: () -> Unit,
) {
    DashboardCard(title = stringResource(R.string.statistics_storage_used_title), onClick = onClick) {
        Text(formatFileSize(stats.totalSize), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.statistics_file_count, stats.totalFileCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DuplicatesCard(
    summary: DuplicatesSummary,
    onClick: () -> Unit,
) {
    DashboardCard(title = stringResource(R.string.statistics_duplicates_title), onClick = onClick) {
        Text(
            stringResource(R.string.dashboard_duplicate_groups_count, summary.groupCount),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InsightsCard(
    flaggedCount: Int,
    onClick: () -> Unit,
) {
    DashboardCard(title = stringResource(R.string.statistics_advisor_title), onClick = onClick) {
        Text(stringResource(R.string.statistics_advisor_flagged_count, flaggedCount), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FavoritesCard(
    entries: List<FileEntry>,
    onClick: () -> Unit,
) {
    DashboardCard(title = stringResource(R.string.dashboard_favorites_title), onClick = onClick) {
        if (entries.isEmpty()) {
            Text(stringResource(R.string.dashboard_favorites_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            entries.forEach { entry -> Text(entry.name, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
