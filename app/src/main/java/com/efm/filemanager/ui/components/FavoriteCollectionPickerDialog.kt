package com.efm.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FavoriteCollection

/**
 * Lets a bulk "add to favorites" action choose which collection the newly-favorited files land
 * in, rather than always defaulting to the root -- docs/PLAN.md Phase 9. Single-select, so
 * tapping a row both picks it and dismisses; [onSelect] gets null for "no collection" (root).
 */
@Composable
fun FavoriteCollectionPickerDialog(
    collections: List<FavoriteCollection>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorite_collection_picker_title)) },
        text = {
            Column {
                CollectionPickerRow(label = stringResource(R.string.favorite_collection_picker_uncategorized)) { onSelect(null) }
                collections.forEach { collection ->
                    CollectionPickerRow(label = collection.name) { onSelect(collection.id) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun CollectionPickerRow(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}
