package com.efm.filemanager.ui.feature.logs

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.data.local.LogEntryEntity

@Composable
fun LogsScreen(
    onOpenDrawer: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
                    }
                },
                title = { Text(stringResource(R.string.nav_logs)) },
            )
        },
    ) { innerPadding ->
        if (entries.isEmpty()) {
            LogsEmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(entries, key = { it.id }) { entry -> LogEntryRow(entry) }
            }
        }
    }
}

@Composable
private fun LogsEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.logs_empty), modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun LogEntryRow(entry: LogEntryEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = logPriorityLabel(entry.priority) + (entry.tag?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelMedium,
            color = logPriorityColor(entry.priority),
        )
        Text(entry.message, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatLogTimestamp(entry.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun logPriorityColor(priority: Int): Color =
    when (priority) {
        Log.ERROR, Log.ASSERT -> MaterialTheme.colorScheme.error
        Log.WARN -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
