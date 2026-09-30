package com.efm.filemanager.ui.feature.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.VaultEntry

@Composable
internal fun VaultListBody(
    entries: List<VaultEntry>,
    selectedIds: SnapshotStateList<Long>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.vault_empty))
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(entries, key = { it.id }) { entry ->
            VaultEntryRow(
                entry = entry,
                isSelected = selectedIds.contains(entry.id),
                onClick = { if (selectedIds.isNotEmpty()) toggleSelection(selectedIds, entry.id) },
                onLongClick = { toggleSelection(selectedIds, entry.id) },
            )
        }
    }
}

private fun toggleSelection(
    selected: SnapshotStateList<Long>,
    id: Long,
) {
    if (selected.contains(id)) selected.remove(id) else selected.add(id)
}
