package com.efm.filemanager.ui.feature.audit

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
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

internal data class AuditTopBarState(
    val hasEntries: Boolean,
    val filter: AuditFilter,
)

internal data class AuditTopBarActions(
    val onOpenDrawer: () -> Unit,
    val onSelectFilter: (AuditFilter) -> Unit,
)

@Composable
internal fun AuditTopBar(
    state: AuditTopBarState,
    actions: AuditTopBarActions,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = actions.onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_audit)) },
        actions = { if (state.hasEntries) AuditTopBarEntryActions(state, actions) },
    )
}

@Composable
private fun AuditTopBarEntryActions(
    state: AuditTopBarState,
    actions: AuditTopBarActions,
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { filterMenuExpanded = true }) {
            Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.audit_filter_menu))
        }
        AuditFilterMenu(
            expanded = filterMenuExpanded,
            selected = state.filter,
            onSelect = actions.onSelectFilter,
            onDismiss = { filterMenuExpanded = false },
        )
    }
}
