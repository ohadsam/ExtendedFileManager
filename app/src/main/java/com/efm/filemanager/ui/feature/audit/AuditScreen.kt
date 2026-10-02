package com.efm.filemanager.ui.feature.audit

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.data.audit.AuditAction
import com.efm.filemanager.data.audit.AuditEventEntity

private const val AUDIT_EXPORT_FILE_NAME = "efm_audit_log.txt"

@Composable
fun AuditScreen(
    onOpenDrawer: () -> Unit,
    viewModel: AuditViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val hasEntries by viewModel.hasEntries.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val tamperedEntry by viewModel.tamperedEntry.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) viewModel.exportTo(uri, buildAuditExportText(context, entries))
        }

    Scaffold(
        topBar = {
            AuditTopBar(
                state =
                    AuditTopBarState(
                        hasEntries = hasEntries,
                        filter = filter,
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                    ),
                actions =
                    AuditTopBarActions(
                        onOpenDrawer = onOpenDrawer,
                        onSelectFilter = viewModel::setFilter,
                        onToggleSearch = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        },
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onExportRequested = { exportLauncher.launch(AUDIT_EXPORT_FILE_NAME) },
                    ),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (tamperedEntry != null) {
                AuditTamperWarning()
            }
            if (entries.isEmpty()) {
                val isFiltered = filter != AuditFilter.ALL || searchQuery.isNotBlank()
                AuditEmptyState(isFiltered = isFiltered, modifier = Modifier.fillMaxWidth())
            } else {
                LazyColumn {
                    items(entries, key = { it.id }) { entry -> AuditEntryRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun AuditTamperWarning() {
    Text(
        text = stringResource(R.string.audit_tamper_warning),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

@Composable
private fun AuditEmptyState(
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(if (isFiltered) R.string.audit_empty_filtered else R.string.audit_empty),
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun AuditEntryRow(entry: AuditEventEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(AuditAction.valueOf(entry.action).labelRes()) + if (entry.success) "" else " · ${failedLabel()}",
            style = MaterialTheme.typography.labelMedium,
            color = if (entry.success) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
        Text(entry.targetName, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatAuditTimestamp(entry.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun failedLabel(): String = stringResource(R.string.audit_failed_label)

private fun buildAuditExportText(
    context: Context,
    entries: List<AuditEventEntity>,
): String {
    val failedLabel = context.getString(R.string.audit_failed_label)
    return entries.joinToString(separator = "\n") { entry ->
        val action = context.getString(AuditAction.valueOf(entry.action).labelRes())
        val status = if (entry.success) "" else " ($failedLabel)"
        "${formatAuditTimestamp(entry.timestamp)} $action$status: ${entry.targetName}"
    }
}
