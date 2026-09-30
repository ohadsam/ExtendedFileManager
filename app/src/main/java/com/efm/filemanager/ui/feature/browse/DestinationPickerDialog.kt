package com.efm.filemanager.ui.feature.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry

/**
 * A folder picker over the app's granted storage tree -- shared by Browse's move/copy and the
 * Vault's export-to-folder, so it takes its title and breadcrumb/folder state as plain
 * parameters rather than Browse's own [PickerUiState], which only Browse's move/copy flow uses.
 */
@Composable
fun DestinationPickerDialog(
    title: String,
    breadcrumbs: List<BreadcrumbEntry>,
    folders: List<FileEntry>,
    isLoading: Boolean,
    onNavigateInto: (FileEntry) -> Unit,
    onNavigateToBreadcrumb: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().height(420.dp).padding(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            PickerBreadcrumbBar(breadcrumbs = breadcrumbs, onCrumbClick = onNavigateToBreadcrumb)
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading && folders.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    else ->
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(folders, key = { it.uri.toString() }) { folder ->
                                PickerFolderRow(folder = folder, onClick = { onNavigateInto(folder) })
                            }
                        }
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                Button(onClick = onConfirm, modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.picker_choose_folder))
                }
            }
        }
    }
}

@Composable
private fun PickerBreadcrumbBar(
    breadcrumbs: List<BreadcrumbEntry>,
    onCrumbClick: (Int) -> Unit,
) {
    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        breadcrumbs.forEachIndexed { index, crumb ->
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onCrumbClick(index) }.padding(end = 4.dp),
            )
            if (index != breadcrumbs.lastIndex) Text("/", modifier = Modifier.padding(end = 4.dp))
        }
    }
}

@Composable
private fun PickerFolderRow(
    folder: FileEntry,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
        Text(folder.name, style = MaterialTheme.typography.bodyLarge)
    }
}
