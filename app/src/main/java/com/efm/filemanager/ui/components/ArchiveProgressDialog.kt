package com.efm.filemanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R

@Composable
fun ArchiveProgressDialog(
    isCompressing: Boolean,
    processedCount: Int,
    onCancel: () -> Unit,
) {
    val titleRes = if (isCompressing) R.string.archive_progress_compressing else R.string.archive_progress_extracting

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 12.dp))
                Text(stringResource(R.string.archive_progress_count, processedCount))
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.archive_progress_cancel)) }
        },
    )
}
