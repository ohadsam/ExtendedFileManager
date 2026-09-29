package com.efm.filemanager.ui.feature.duplicates

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.DuplicateGroup
import com.efm.filemanager.ui.components.FileRow
import com.efm.filemanager.ui.feature.browse.formatFileSize

@Composable
internal fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedUris: SnapshotStateList<Uri>,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.duplicate_group_summary, group.files.size, formatFileSize(group.fileSize)),
                    style = MaterialTheme.typography.titleSmall,
                )
                TextButton(onClick = { selectAllButFirst(group, selectedUris) }) {
                    Text(stringResource(R.string.duplicate_keep_one))
                }
            }
            group.files.forEach { entry ->
                FileRow(
                    entry = entry,
                    isSelected = selectedUris.contains(entry.uri),
                    onClick = { toggleSelection(selectedUris, entry.uri) },
                    onLongClick = {},
                )
            }
        }
    }
}

private fun selectAllButFirst(
    group: DuplicateGroup,
    selectedUris: SnapshotStateList<Uri>,
) {
    group.files.drop(1).forEach { entry -> if (!selectedUris.contains(entry.uri)) selectedUris.add(entry.uri) }
}

private fun toggleSelection(
    selectedUris: SnapshotStateList<Uri>,
    uri: Uri,
) {
    if (selectedUris.contains(uri)) selectedUris.remove(uri) else selectedUris.add(uri)
}
