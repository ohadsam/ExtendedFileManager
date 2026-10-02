package com.efm.filemanager.ui.feature.logs

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.data.local.LogEntryEntity

private const val LOGS_EXPORT_FILE_NAME = "efm_logs.txt"

@Composable
fun LogsScreen(
    onOpenDrawer: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val hasEntries by viewModel.hasEntries.collectAsStateWithLifecycle()
    val priorityFilter by viewModel.priorityFilter.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) viewModel.exportTo(uri)
        }

    Scaffold(
        topBar = {
            LogsTopBar(
                state = LogsTopBarState(hasEntries = hasEntries, priorityFilter = priorityFilter),
                actions =
                    LogsTopBarActions(
                        onOpenDrawer = onOpenDrawer,
                        onSelectFilter = viewModel::setPriorityFilter,
                        onClearRequested = { showClearConfirm = true },
                        onExportRequested = { exportLauncher.launch(LOGS_EXPORT_FILE_NAME) },
                    ),
            )
        },
    ) { innerPadding ->
        if (entries.isEmpty()) {
            LogsEmptyState(isFiltered = priorityFilter != LogPriorityFilter.ALL, modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(entries, key = { it.id }) { entry -> LogEntryRow(entry) }
            }
        }
    }

    if (showClearConfirm) {
        LogsClearConfirmDialog(
            onConfirm = {
                viewModel.clearAll()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

@Composable
private fun LogsEmptyState(
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(if (isFiltered) R.string.logs_empty_filtered else R.string.logs_empty),
            modifier = Modifier.padding(24.dp),
        )
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
