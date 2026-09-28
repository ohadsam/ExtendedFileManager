package com.efm.filemanager.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileGroup
import com.efm.filemanager.domain.model.GroupKey
import com.efm.filemanager.ui.components.query.displayLabel

/** Renders [groups] as a flat list with a header per non-empty [GroupKey] -- shared by Browse and Search. */
@Composable
fun GroupedFileList(
    modifier: Modifier = Modifier,
    groups: List<FileGroup>,
    selectedUris: List<Uri>,
    onEntryClick: (FileEntry) -> Unit,
    onEntryLongClick: (FileEntry) -> Unit,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        groups.forEach { group ->
            if (group.key != GroupKey.None) {
                item(key = "header_${group.key}") { GroupHeader(group.key) }
            }
            items(group.files, key = { it.uri.toString() }) { entry ->
                FileRow(
                    entry = entry,
                    isSelected = selectedUris.contains(entry.uri),
                    onClick = { onEntryClick(entry) },
                    onLongClick = { onEntryLongClick(entry) },
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(key: GroupKey) {
    Text(
        text = key.displayLabel(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
