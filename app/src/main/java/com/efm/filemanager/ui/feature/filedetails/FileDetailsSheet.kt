package com.efm.filemanager.ui.feature.filedetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.ui.components.TagChip
import com.efm.filemanager.ui.components.TagPickerDialog

/** The one screen to see and edit a single file's favorite/tags/lock/note at once -- docs/PLAN.md Phase 9. */
@Composable
fun FileDetailsSheet(
    entry: FileEntry,
    onDismiss: () -> Unit,
    onOpenManageTags: () -> Unit,
    viewModel: FileDetailsViewModel = hiltViewModel(),
) {
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    var isFavorite by remember(entry.uri) { mutableStateOf(entry.isFavorite) }
    var isLocked by remember(entry.uri) { mutableStateOf(entry.isLocked) }
    var note by remember(entry.uri) { mutableStateOf(entry.note.orEmpty()) }
    var tags by remember(entry.uri) { mutableStateOf(entry.tags) }
    var tagPickerVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                FavoriteRow(
                    isFavorite = isFavorite,
                    onToggle = { checked ->
                        isFavorite = checked
                        if (checked) viewModel.setFavorite(entry.uri, null) else viewModel.removeFavorite(entry.uri)
                    },
                )
                LockRow(
                    isLocked = isLocked,
                    onToggle = { checked ->
                        isLocked = checked
                        viewModel.setLocked(entry.uri, checked)
                    },
                )

                SectionLabel(R.string.file_details_tags)
                TagsSection(
                    tags = tags,
                    onAddClick = { tagPickerVisible = true },
                    onRemove = { tag ->
                        tags = tags.filterNot { it.id == tag.id }
                        viewModel.removeTag(entry.uri, tag.id)
                    },
                )

                SectionLabel(R.string.file_details_note)
                OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                LaunchedEffect(note) { viewModel.setNote(entry.uri, note) }

                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }

    if (tagPickerVisible) {
        TagPickerDialog(
            tags = allTags,
            onApply = { selectedIds ->
                selectedIds.forEach { tagId -> viewModel.addTag(entry.uri, tagId) }
                val newlyAdded = allTags.filter { it.id in selectedIds && tags.none { existing -> existing.id == it.id } }
                tags = tags + newlyAdded
                tagPickerVisible = false
            },
            onManageTags = {
                tagPickerVisible = false
                onOpenManageTags()
            },
            onDismiss = { tagPickerVisible = false },
        )
    }
}

@Composable
private fun SectionLabel(labelRes: Int) {
    Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun FavoriteRow(
    isFavorite: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.file_details_favorite), modifier = Modifier.weight(1f))
        Switch(checked = isFavorite, onCheckedChange = onToggle)
    }
}

@Composable
private fun LockRow(
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.file_details_lock), modifier = Modifier.weight(1f))
        Switch(checked = isLocked, onCheckedChange = onToggle)
    }
}

@Composable
private fun TagsSection(
    tags: List<FileTag>,
    onAddClick: () -> Unit,
    onRemove: (FileTag) -> Unit,
) {
    LazyRow {
        items(tags, key = { it.id }) { tag ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                TagChip(tag)
                IconButton(onClick = { onRemove(tag) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.file_details_remove_tag, tag.name))
                }
            }
        }
        item {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_tag))
            }
        }
    }
}
