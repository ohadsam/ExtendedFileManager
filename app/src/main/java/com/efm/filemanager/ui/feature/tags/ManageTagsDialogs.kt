package com.efm.filemanager.ui.feature.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.TagColor
import com.efm.filemanager.ui.components.ConfirmDangerousActionDialog
import com.efm.filemanager.ui.components.TagColorPicker

@Composable
internal fun ManageTagsDialogHost(
    dialog: ManageTagsDialog?,
    allTags: List<FileTag>,
    viewModel: ManageTagsViewModel,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        ManageTagsDialog.Create -> CreateTagDialog(viewModel, onDismiss)
        is ManageTagsDialog.Edit -> EditTagDialog(dialog.tag, viewModel, onDismiss)
        is ManageTagsDialog.Merge -> MergeDialog(dialog.tag, allTags, viewModel, onDismiss)
        is ManageTagsDialog.Delete -> DeleteTagDialog(dialog.tag, viewModel, onDismiss)
        null -> Unit
    }
}

@Composable
private fun CreateTagDialog(
    viewModel: ManageTagsViewModel,
    onDismiss: () -> Unit,
) {
    TagEditDialog(
        titleRes = R.string.manage_tags_create,
        initialName = "",
        initialColor = TagColor.BLUE,
        onConfirm = { name, color ->
            viewModel.createTag(name, color)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun EditTagDialog(
    tag: FileTag,
    viewModel: ManageTagsViewModel,
    onDismiss: () -> Unit,
) {
    TagEditDialog(
        titleRes = R.string.manage_tags_edit,
        initialName = tag.name,
        initialColor = tag.color,
        onConfirm = { name, color ->
            viewModel.updateTag(tag, name, color)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun DeleteTagDialog(
    tag: FileTag,
    viewModel: ManageTagsViewModel,
    onDismiss: () -> Unit,
) {
    ConfirmDangerousActionDialog(
        title = stringResource(R.string.manage_tags_delete_title),
        message = stringResource(R.string.manage_tags_delete_message, tag.name),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = {
            viewModel.deleteTag(tag)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun TagEditDialog(
    titleRes: Int,
    initialName: String,
    initialColor: TagColor,
    onConfirm: (String, TagColor) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
                TagColorPicker(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.manage_tags_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun MergeDialog(
    tag: FileTag,
    allTags: List<FileTag>,
    viewModel: ManageTagsViewModel,
    onDismiss: () -> Unit,
) {
    val otherTags = allTags.filter { it.id != tag.id }
    var target by remember { mutableStateOf(otherTags.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_tags_merge)) },
        text = {
            Column {
                Text(stringResource(R.string.manage_tags_merge_body, tag.name))
                otherTags.forEach { candidate ->
                    MergeTargetRow(candidate = candidate, selected = candidate == target, onSelect = { target = candidate })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val destination = target ?: return@TextButton
                    viewModel.mergeTags(tag, destination)
                    onDismiss()
                },
                enabled = target != null,
            ) {
                Text(stringResource(R.string.manage_tags_merge))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun MergeTargetRow(
    candidate: FileTag,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row {
        RadioButton(selected = selected, onClick = onSelect)
        Text(candidate.name)
    }
}
