package com.efm.filemanager.ui.feature.filedetails

import android.net.Uri
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

/** [FileDetailsSheet]'s locally-edited fields, bundled so the sheet can pass them as one param. */
private class FileDetailsFields(
    initialFavorite: Boolean,
    initialLocked: Boolean,
    initialNote: String,
    initialTags: List<FileTag>,
) {
    var isFavorite by mutableStateOf(initialFavorite)
    var isLocked by mutableStateOf(initialLocked)
    var note by mutableStateOf(initialNote)
    var tags by mutableStateOf(initialTags)
}

@Composable
private fun rememberFileDetailsFields(entry: FileEntry): FileDetailsFields =
    remember(entry.uri) { FileDetailsFields(entry.isFavorite, entry.isLocked, entry.note.orEmpty(), entry.tags) }

/** The one screen to see and edit a single file's favorite/tags/lock/note at once -- docs/PLAN.md Phase 9. */
@Composable
fun FileDetailsSheet(
    entry: FileEntry,
    onDismiss: () -> Unit,
    onOpenManageTags: () -> Unit,
    viewModel: FileDetailsViewModel = hiltViewModel(),
) {
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val fields = rememberFileDetailsFields(entry)
    var tagPickerVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        FileDetailsCard(
            entry = entry,
            fields = fields,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onAddTagClick = { tagPickerVisible = true },
        )
    }

    if (tagPickerVisible) {
        FileDetailsTagPicker(
            fileUri = entry.uri,
            allTags = allTags,
            currentTags = fields.tags,
            viewModel = viewModel,
            callbacks =
                FileDetailsTagPickerCallbacks(
                    onTagsAdded = { newlyAdded -> fields.tags = fields.tags + newlyAdded },
                    onOpenManageTags = onOpenManageTags,
                    onDismiss = { tagPickerVisible = false },
                ),
        )
    }
}

@Composable
private fun FileDetailsCard(
    entry: FileEntry,
    fields: FileDetailsFields,
    viewModel: FileDetailsViewModel,
    onDismiss: () -> Unit,
    onAddTagClick: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            FavoriteRow(
                isFavorite = fields.isFavorite,
                onToggle = { checked ->
                    fields.isFavorite = checked
                    if (checked) viewModel.setFavorite(entry.uri, null) else viewModel.removeFavorite(entry.uri)
                },
            )
            LockRow(
                isLocked = fields.isLocked,
                onToggle = { checked ->
                    fields.isLocked = checked
                    viewModel.setLocked(entry.uri, checked)
                },
            )

            SectionLabel(R.string.file_details_tags)
            TagsSection(
                tags = fields.tags,
                onAddClick = onAddTagClick,
                onRemove = { tag ->
                    fields.tags = fields.tags.filterNot { it.id == tag.id }
                    viewModel.removeTag(entry.uri, tag.id)
                },
            )

            SectionLabel(R.string.file_details_note)
            OutlinedTextField(
                value = fields.note,
                onValueChange = { fields.note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            LaunchedEffect(fields.note) { viewModel.setNote(entry.uri, fields.note) }

            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

private data class FileDetailsTagPickerCallbacks(
    val onTagsAdded: (List<FileTag>) -> Unit,
    val onOpenManageTags: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
private fun FileDetailsTagPicker(
    fileUri: Uri,
    allTags: List<FileTag>,
    currentTags: List<FileTag>,
    viewModel: FileDetailsViewModel,
    callbacks: FileDetailsTagPickerCallbacks,
) {
    TagPickerDialog(
        tags = allTags,
        onApply = { selectedIds ->
            selectedIds.forEach { tagId -> viewModel.addTag(fileUri, tagId) }
            val newlyAdded = allTags.filter { it.id in selectedIds && currentTags.none { existing -> existing.id == it.id } }
            callbacks.onTagsAdded(newlyAdded)
            callbacks.onDismiss()
        },
        onManageTags = {
            callbacks.onDismiss()
            callbacks.onOpenManageTags()
        },
        onDismiss = callbacks.onDismiss,
    )
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
