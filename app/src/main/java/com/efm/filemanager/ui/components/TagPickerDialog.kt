package com.efm.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileTag

/** Multi-select tag picker for the bulk "add tag" quick-action -- [tags] arrives already pinned-first. */
@Composable
fun TagPickerDialog(
    tags: List<FileTag>,
    onApply: (Set<Long>) -> Unit,
    onManageTags: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_picker_title)) },
        text = {
            Column {
                if (tags.isEmpty()) {
                    Text(stringResource(R.string.tag_picker_empty))
                } else {
                    tags.forEach { tag ->
                        TagPickerRow(
                            tag = tag,
                            checked = tag.id in selected,
                            onToggle = { selected = if (tag.id in selected) selected - tag.id else selected + tag.id },
                        )
                    }
                }
                TextButton(onClick = onManageTags) { Text(stringResource(R.string.tag_picker_manage)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selected) }, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.tag_picker_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun TagPickerRow(
    tag: FileTag,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        TagChip(tag)
    }
}
