package com.efm.filemanager.ui.feature.whatsnew

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R

@Composable
fun WhatsNewDialog(
    entries: List<WhatsNewEntry>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title)) },
        text = {
            Column {
                entries.forEach { entry -> WhatsNewEntryBody(entry) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.whats_new_got_it)) }
        },
    )
}

@Composable
private fun WhatsNewEntryBody(entry: WhatsNewEntry) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(stringResource(entry.titleRes), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(entry.bodyRes), style = MaterialTheme.typography.bodyMedium)
    }
}
