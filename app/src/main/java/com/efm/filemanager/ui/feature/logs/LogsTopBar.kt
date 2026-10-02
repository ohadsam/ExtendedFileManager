package com.efm.filemanager.ui.feature.logs

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R

internal data class LogsTopBarState(
    val hasEntries: Boolean,
    val priorityFilter: LogPriorityFilter,
)

internal data class LogsTopBarActions(
    val onOpenDrawer: () -> Unit,
    val onSelectFilter: (LogPriorityFilter) -> Unit,
    val onClearRequested: () -> Unit,
    val onExportRequested: () -> Unit,
)

@Composable
internal fun LogsTopBar(
    state: LogsTopBarState,
    actions: LogsTopBarActions,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = actions.onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_logs)) },
        actions = { if (state.hasEntries) LogsTopBarEntryActions(state, actions) },
    )
}

@Composable
private fun LogsTopBarEntryActions(
    state: LogsTopBarState,
    actions: LogsTopBarActions,
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { filterMenuExpanded = true }) {
            Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.logs_filter_menu))
        }
        LogsFilterMenu(
            expanded = filterMenuExpanded,
            selected = state.priorityFilter,
            onSelect = actions.onSelectFilter,
            onDismiss = { filterMenuExpanded = false },
        )
    }
    IconButton(onClick = actions.onExportRequested) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.logs_export_action))
    }
    IconButton(onClick = actions.onClearRequested) {
        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.logs_clear_action))
    }
}
