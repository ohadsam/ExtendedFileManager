package com.efm.filemanager.ui.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.data.dashboard.DashboardSavedLayout
import com.efm.filemanager.data.dashboard.DashboardWidgetConfig
import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.domain.model.DashboardTemplate
import com.efm.filemanager.domain.model.DashboardWidgetSize
import com.efm.filemanager.domain.model.DashboardWidgetType
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.StorageStats
import com.efm.filemanager.domain.model.toggled
import com.efm.filemanager.ui.components.query.QueryMenuSectionHeader
import com.efm.filemanager.ui.components.query.SelectableMenuItem
import com.efm.filemanager.ui.feature.browse.formatFileSize
import com.efm.filemanager.ui.feature.statistics.DuplicatesSummary

@Composable
fun DashboardScreen(
    onOpenDrawer: () -> Unit,
    navActions: DashboardNavActions,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<DashboardDialog?>(null) }
    Scaffold(
        topBar = {
            DashboardTopBar(
                onOpenDrawer = onOpenDrawer,
                onOpenSearch = navActions.onOpenSearch,
                isEditMode = uiState.isEditMode,
                onToggleEditMode = viewModel::toggleEditMode,
                layoutsMenuState =
                    LayoutsMenuState(
                        savedLayouts = uiState.savedLayouts,
                        onApplyTemplate = viewModel::applyTemplate,
                        onApplySavedLayout = viewModel::applySavedLayout,
                        onSaveCurrentClick = { dialog = DashboardDialog.SaveLayout },
                        onRenameSavedLayout = { layout -> dialog = DashboardDialog.RenameLayout(layout) },
                        onDeleteSavedLayout = { layout -> dialog = DashboardDialog.DeleteLayout(layout) },
                    ),
            )
        },
    ) { innerPadding ->
        if (uiState.isEditMode) {
            DashboardEditList(
                modifier = Modifier.padding(innerPadding),
                configs = uiState.widgetConfigs,
                onToggle = viewModel::setWidgetEnabled,
                onMove = viewModel::moveWidget,
                onToggleSize = viewModel::setWidgetSize,
            )
        } else {
            DashboardBody(modifier = Modifier.padding(innerPadding), uiState = uiState, navActions = navActions)
        }
    }
    DashboardDialogHost(dialog = dialog, viewModel = viewModel, onDismiss = { dialog = null })
}

@Composable
private fun DashboardTopBar(
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    layoutsMenuState: LayoutsMenuState,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_dashboard)) },
        actions = {
            // Layouts only make sense while customizing -- applying one is itself an edit.
            if (isEditMode) {
                LayoutsMenuButton(layoutsMenuState)
            }
            IconButton(onClick = onToggleEditMode) {
                Icon(
                    if (isEditMode) Icons.Filled.Done else Icons.Filled.Edit,
                    contentDescription =
                        stringResource(if (isEditMode) R.string.dashboard_edit_done else R.string.dashboard_edit_start),
                )
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_icon))
            }
        },
    )
}

/** Bundled so [DashboardTopBar] stays under detekt's `LongParameterList` threshold. */
private data class LayoutsMenuState(
    val savedLayouts: List<DashboardSavedLayout>,
    val onApplyTemplate: (DashboardTemplate) -> Unit,
    val onApplySavedLayout: (DashboardSavedLayout) -> Unit,
    val onSaveCurrentClick: () -> Unit,
    val onRenameSavedLayout: (DashboardSavedLayout) -> Unit,
    val onDeleteSavedLayout: (DashboardSavedLayout) -> Unit,
)

/**
 * Covers both halves of the spec's single "layout picker": the built-in [DashboardTemplate]s and
 * the user's own named saves. Neither section is a "current selection" list -- applying either
 * one is a one-shot rewrite, same as tapping a reset, never a sticky choice that stays checked.
 */
@Composable
private fun LayoutsMenuButton(state: LayoutsMenuState) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.List, contentDescription = stringResource(R.string.dashboard_layouts_action))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dashboard_save_layout_action)) },
                onClick = {
                    expanded = false
                    state.onSaveCurrentClick()
                },
            )
            QueryMenuSectionHeader(stringResource(R.string.dashboard_templates_section))
            DashboardTemplate.entries.forEach { template ->
                SelectableMenuItem(
                    labelRes = template.labelRes(),
                    selected = false,
                    onDismiss = { expanded = false },
                    onClick = { state.onApplyTemplate(template) },
                )
            }
            if (state.savedLayouts.isNotEmpty()) {
                QueryMenuSectionHeader(stringResource(R.string.dashboard_saved_layouts_section))
                state.savedLayouts.forEach { layout ->
                    SavedLayoutMenuItem(
                        layout = layout,
                        onApply = {
                            expanded = false
                            state.onApplySavedLayout(layout)
                        },
                        onRename = {
                            expanded = false
                            state.onRenameSavedLayout(layout)
                        },
                        onDelete = {
                            expanded = false
                            state.onDeleteSavedLayout(layout)
                        },
                    )
                }
            }
        }
    }
}

/** Extracted from [LayoutsMenuButton] to stay under detekt's `LongMethod` threshold. */
@Composable
private fun SavedLayoutMenuItem(
    layout: DashboardSavedLayout,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(layout.name) },
        onClick = onApply,
        trailingIcon = {
            Row {
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename_title))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
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
    val visibleConfigs = uiState.widgetConfigs.filter { it.isEnabled }
    if (visibleConfigs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.dashboard_all_hidden))
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(visibleConfigs, key = { it.type.name }) { config ->
            DashboardWidgetRow(config, uiState, navActions)
        }
    }
}

@Composable
private fun DashboardWidgetRow(
    config: DashboardWidgetConfig,
    uiState: DashboardUiState,
    navActions: DashboardNavActions,
) {
    when (config.type) {
        DashboardWidgetType.STORAGE_SUMMARY ->
            StorageSummaryCard(uiState.storageStats, config.size, onClick = navActions.onOpenStatistics)
        DashboardWidgetType.DUPLICATES ->
            DuplicatesCard(uiState.duplicatesSummary, config.size, onClick = navActions.onOpenDuplicates)
        // No size-dependent content yet -- there's nothing more for this card to show or trim.
        DashboardWidgetType.INSIGHTS -> InsightsCard(uiState.advisorFlaggedCount, onClick = navActions.onOpenInsights)
        DashboardWidgetType.FAVORITES ->
            FavoritesCard(uiState.favoriteEntries, config.size, onClick = navActions.onOpenFavorites)
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
    size: DashboardWidgetSize,
    onClick: () -> Unit,
) {
    DashboardCard(title = stringResource(R.string.statistics_storage_used_title), onClick = onClick) {
        Text(formatFileSize(stats.totalSize), style = MaterialTheme.typography.headlineSmall)
        if (size == DashboardWidgetSize.DETAILED) {
            Text(
                stringResource(R.string.statistics_file_count, stats.totalFileCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DuplicatesCard(
    summary: DuplicatesSummary,
    size: DashboardWidgetSize,
    onClick: () -> Unit,
) {
    DashboardCard(title = stringResource(R.string.statistics_duplicates_title), onClick = onClick) {
        Text(
            stringResource(R.string.dashboard_duplicate_groups_count, summary.groupCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (size == DashboardWidgetSize.DETAILED) {
            Text(
                stringResource(R.string.dashboard_reclaimable_detail, formatFileSize(summary.reclaimableBytes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    size: DashboardWidgetSize,
    onClick: () -> Unit,
) {
    DashboardCard(title = stringResource(R.string.dashboard_favorites_title), onClick = onClick) {
        when {
            entries.isEmpty() -> Text(stringResource(R.string.dashboard_favorites_empty), style = MaterialTheme.typography.bodySmall)
            size == DashboardWidgetSize.COMPACT ->
                Text(stringResource(R.string.dashboard_favorites_count, entries.size), style = MaterialTheme.typography.bodyMedium)
            else -> entries.forEach { entry -> Text(entry.name, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

private val WIDGET_ROW_HEIGHT = 56.dp

/**
 * A widget being dragged by its handle -- [offsetY] accumulates raw drag distance and every full
 * [WIDGET_ROW_HEIGHT] crossed triggers one adjacent swap via the same [DashboardViewModel.moveWidget]
 * a future up/down affordance could use too, so dragging is a real, continuous reorder gesture
 * built on already-exercised logic rather than a from-scratch reimplementation -- the exact
 * technique `FavoritesScreen`'s own `CollectionDragState` already established.
 */
private class WidgetDragState {
    var draggingType by mutableStateOf<DashboardWidgetType?>(null)
    var offsetY by mutableFloatStateOf(0f)
}

private fun onDragCrossedWidgetRow(
    dragState: WidgetDragState,
    rowHeightPx: Float,
    index: Int,
    configs: List<DashboardWidgetConfig>,
    onMove: (DashboardWidgetType, MoveDirection) -> Unit,
) {
    if (dragState.offsetY > rowHeightPx && index < configs.lastIndex) {
        onMove(configs[index].type, MoveDirection.DOWN)
        dragState.offsetY -= rowHeightPx
    } else if (dragState.offsetY < -rowHeightPx && index > 0) {
        onMove(configs[index].type, MoveDirection.UP)
        dragState.offsetY += rowHeightPx
    }
}

/** Bundled so [WidgetEditRow] stays under detekt's `LongParameterList` threshold. */
private data class RowDragVisuals(
    val offsetY: Float,
    val isDragging: Boolean,
)

/** Also bundled for the same reason -- a row's two non-drag actions, visibility and size. */
private data class WidgetEditCallbacks(
    val onToggleEnabled: (Boolean) -> Unit,
    val onToggleSize: () -> Unit,
)

/**
 * The edit-mode view: a compact, fixed-height row per widget (so the drag-crossed-row threshold
 * below is meaningful), covering every catalog entry -- disabled ones included, dimmed -- since
 * this is the only place a hidden widget can be turned back on.
 */
@Composable
private fun DashboardEditList(
    modifier: Modifier,
    configs: List<DashboardWidgetConfig>,
    onToggle: (DashboardWidgetType, Boolean) -> Unit,
    onMove: (DashboardWidgetType, MoveDirection) -> Unit,
    onToggleSize: (DashboardWidgetType, DashboardWidgetSize) -> Unit,
) {
    val dragState = remember { WidgetDragState() }
    val rowHeightPx = with(LocalDensity.current) { WIDGET_ROW_HEIGHT.toPx() }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(configs, key = { _, config -> config.type.name }) { index, config ->
            val isDragging = dragState.draggingType == config.type
            WidgetEditRow(
                config = config,
                dragVisuals = RowDragVisuals(offsetY = if (isDragging) dragState.offsetY else 0f, isDragging = isDragging),
                callbacks =
                    WidgetEditCallbacks(
                        onToggleEnabled = { enabled -> onToggle(config.type, enabled) },
                        onToggleSize = { onToggleSize(config.type, config.size.toggled()) },
                    ),
                onDrag = { delta ->
                    dragState.draggingType = config.type
                    dragState.offsetY += delta
                    onDragCrossedWidgetRow(dragState, rowHeightPx, index, configs, onMove)
                },
                onDragEnd = {
                    dragState.draggingType = null
                    dragState.offsetY = 0f
                },
            )
        }
    }
}

@Composable
private fun WidgetEditRow(
    config: DashboardWidgetConfig,
    dragVisuals: RowDragVisuals,
    callbacks: WidgetEditCallbacks,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(WIDGET_ROW_HEIGHT)
                .graphicsLayer { translationY = dragVisuals.offsetY }
                .zIndex(if (dragVisuals.isDragging) 1f else 0f)
                .padding(horizontal = 16.dp),
    ) {
        WidgetDragHandle(onDrag = onDrag, onDragEnd = onDragEnd)
        Text(
            stringResource(config.type.titleRes()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).alpha(if (config.isEnabled) 1f else 0.5f),
        )
        IconButton(onClick = callbacks.onToggleSize) {
            Icon(
                if (config.size == DashboardWidgetSize.DETAILED) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                contentDescription =
                    stringResource(
                        if (config.size == DashboardWidgetSize.DETAILED) {
                            R.string.dashboard_size_make_compact
                        } else {
                            R.string.dashboard_size_make_detailed
                        },
                    ),
            )
        }
        Switch(checked = config.isEnabled, onCheckedChange = callbacks.onToggleEnabled)
    }
}

/**
 * Same [rememberUpdatedState] staleness fix `FavoritesScreen.DragHandleIcon` documents -- this
 * row is reused in place across a reorder, not recreated.
 */
@Composable
private fun WidgetDragHandle(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    Icon(
        imageVector = Icons.Filled.DragHandle,
        contentDescription = stringResource(R.string.favorites_reorder_handle),
        modifier =
            Modifier
                .padding(end = 12.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragEnd() },
                    ) { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.y)
                    }
                },
    )
}
