package com.efm.filemanager.ui.feature.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FolderFileCount
import com.efm.filemanager.domain.model.GlobalFilesSort
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.ui.components.query.labelRes
import com.efm.filemanager.ui.feature.browse.formatDate
import com.efm.filemanager.ui.feature.browse.formatFileSize

@Composable
fun StatisticsScreen(
    onOpenDrawer: () -> Unit,
    navActions: StatisticsNavActions,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { StatisticsTopBar(onOpenDrawer = onOpenDrawer, onRefresh = viewModel::refresh) },
    ) { innerPadding ->
        StatisticsBody(modifier = Modifier.padding(innerPadding), uiState = uiState, navActions = navActions)
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
    navActions: StatisticsNavActions,
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
        item { StorageUsedCard(uiState.storageStats, uiState.storageTrend) }
        item {
            FileTypeBreakdownCard(
                uiState.storageStats.sizeByCategory,
                onCategoryClick = { category -> navActions.onOpenGlobalFiles(category, GlobalFilesSort.SIZE) },
            )
        }
        item {
            LargestFilesCard(uiState.storageStats.largestFiles, onClick = { navActions.onOpenGlobalFiles(null, GlobalFilesSort.SIZE) })
        }
        item {
            RecentlyModifiedCard(
                uiState.storageStats.recentlyModifiedFiles,
                onClick = { navActions.onOpenGlobalFiles(null, GlobalFilesSort.RECENT) },
            )
        }
        item {
            MostPopulatedFoldersCard(uiState.storageStats.mostPopulatedFolders, onFolderClick = navActions.onOpenFolder)
        }
        item {
            DuplicatesStatCard(uiState.duplicatesSummary, onClick = navActions.onOpenDuplicates)
        }
        item { AdvisorStatCard(uiState.advisorFlaggedCount, onClick = navActions.onOpenInsights) }
        item { OperationsStatCard(uiState.totalOperationsCount, onClick = navActions.onOpenAudit) }
    }
}

@Composable
private fun StatCard(
    title: String,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cardModifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    ElevatedCard(modifier = cardModifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun StorageUsedCard(
    stats: StorageStats,
    trend: List<Long>,
) {
    StatCard(title = stringResource(R.string.statistics_storage_used_title)) {
        Text(formatFileSize(stats.totalSize), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.statistics_file_count, stats.totalFileCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (trend.size >= 2) {
            Spacer(modifier = Modifier.height(8.dp))
            StorageTrendSparkline(trend)
        }
    }
}

/**
 * A minimal sparkline -- a thin single-hue line, no axes/gridlines/labels -- the form this
 * phase's own spec calls for pairing a trend with a stat tile, per the dataviz skill's own
 * "single current value + maybe a trend -> stat tile with sparkline" guidance. Needs at least
 * two points; the caller already guards that, since this history is forward-built from whenever
 * Phase 18 landed and may still be "not enough data yet."
 */
@Composable
private fun StorageTrendSparkline(dailyTotals: List<Long>) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(SPARKLINE_HEIGHT)) {
        val maxValue = dailyTotals.max().toFloat()
        val minValue = dailyTotals.min().toFloat()
        val range = (maxValue - minValue).coerceAtLeast(1f)
        val stepX = size.width / (dailyTotals.size - 1)
        val path =
            Path().apply {
                dailyTotals.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - ((value.toFloat() - minValue) / range) * size.height
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
        val stroke = Stroke(width = SPARKLINE_STROKE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path, color = lineColor, style = stroke)
    }
}

private val SPARKLINE_HEIGHT = 32.dp
private val SPARKLINE_STROKE_WIDTH = 2.dp

@Composable
private fun FileTypeBreakdownCard(
    sizeByCategory: Map<FileCategory, Long>,
    onCategoryClick: (FileCategory) -> Unit,
) {
    StatCard(title = stringResource(R.string.statistics_by_type_title)) {
        if (sizeByCategory.isEmpty()) {
            Text(stringResource(R.string.statistics_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            val sorted = sizeByCategory.entries.sortedByDescending { it.value }
            StackedShareBar(sorted, onCategoryClick = onCategoryClick)
            Spacer(modifier = Modifier.height(8.dp))
            sorted.forEach { (category, size) ->
                StatRow(
                    label = stringResource(category.labelRes()),
                    value = formatFileSize(size),
                    swatchColor = category.chartColor(),
                    onClick = { onCategoryClick(category) },
                )
            }
        }
    }
}

/**
 * A horizontal stacked bar, not a donut -- a part-to-whole breakdown with up to seven slices
 * needs only the adjacent-pairs color gate a stacked bar's touching segments require, not the
 * stricter all-pairs gate every slice of a simultaneously-visible pie/donut would need (see
 * [chartColor]'s doc for the validated numbers). A 2dp gap separates segments, matching the
 * dataviz skill's "2px surface gap between stacked fills" mark spec. Each segment is tappable,
 * same drill-down as its matching legend row below.
 */
@Composable
private fun StackedShareBar(
    sortedEntries: List<Map.Entry<FileCategory, Long>>,
    onCategoryClick: (FileCategory) -> Unit,
) {
    val total = sortedEntries.sumOf { it.value }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        sortedEntries.forEach { (category, size) ->
            val share = if (total > 0) size.toFloat() / total.toFloat() else 0f
            if (share > 0f) {
                Box(
                    modifier =
                        Modifier
                            .weight(share)
                            .fillMaxHeight()
                            .background(category.chartColor())
                            .clickable { onCategoryClick(category) },
                )
            }
        }
    }
}

@Composable
private fun LargestFilesCard(
    largestFiles: List<FileEntry>,
    onClick: () -> Unit,
) {
    StatCard(title = stringResource(R.string.statistics_largest_files_title), onClick = onClick) {
        if (largestFiles.isEmpty()) {
            Text(stringResource(R.string.statistics_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            largestFiles.forEach { file -> StatRow(label = file.name, value = formatFileSize(file.size)) }
        }
    }
}

@Composable
private fun RecentlyModifiedCard(
    recentlyModifiedFiles: List<FileEntry>,
    onClick: () -> Unit,
) {
    StatCard(title = stringResource(R.string.statistics_recently_modified_title), onClick = onClick) {
        if (recentlyModifiedFiles.isEmpty()) {
            Text(stringResource(R.string.statistics_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            recentlyModifiedFiles.forEach { file -> StatRow(label = file.name, value = formatDate(file.lastModified)) }
        }
    }
}

/**
 * Each row drills into its own folder (unlike every other widget here, which drills into one
 * shared destination), so the per-row [StatRow.onClick] carries the folder, not a whole-card one.
 * Reaches Browse via the same `browseViewModel`-hoisted-in-`EfmApp` + navigate-and-restore pattern
 * `EfmSearchDestination`'s own drill-down uses -- this widget shipped without it originally only
 * because that pattern didn't exist yet (Statistics pops Browse off the back stack via `popUpTo`,
 * so the older `getBackStackEntry(Browse.route)` trick Search alone could still rely on never
 * worked here).
 */
@Composable
private fun MostPopulatedFoldersCard(
    mostPopulatedFolders: List<FolderFileCount>,
    onFolderClick: (String) -> Unit,
) {
    StatCard(title = stringResource(R.string.statistics_most_populated_folders_title)) {
        if (mostPopulatedFolders.isEmpty()) {
            Text(stringResource(R.string.statistics_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            mostPopulatedFolders.forEach { folder ->
                StatRow(
                    label = folder.name,
                    value = stringResource(R.string.statistics_folder_file_count, folder.fileCount),
                    onClick = { onFolderClick(folder.uri) },
                )
            }
        }
    }
}

@Composable
private fun DuplicatesStatCard(
    summary: DuplicatesSummary,
    onClick: () -> Unit,
) {
    StatCard(title = stringResource(R.string.statistics_duplicates_title), onClick = onClick) {
        StatRow(label = stringResource(R.string.statistics_duplicate_groups), value = summary.groupCount.toString())
        StatRow(label = stringResource(R.string.statistics_reclaimable_space), value = formatFileSize(summary.reclaimableBytes))
    }
}

@Composable
private fun AdvisorStatCard(
    flaggedCount: Int,
    onClick: () -> Unit,
) {
    StatCard(title = stringResource(R.string.statistics_advisor_title), onClick = onClick) {
        Text(stringResource(R.string.statistics_advisor_flagged_count, flaggedCount), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun OperationsStatCard(
    totalCount: Int,
    onClick: () -> Unit,
) {
    StatCard(title = stringResource(R.string.statistics_operations_title), onClick = onClick) {
        Text(stringResource(R.string.statistics_operations_count, totalCount), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    swatchColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (swatchColor != null) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(swatchColor))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
