package com.efm.filemanager.ui.feature.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.data.cloud.CloudUploadStatus
import com.efm.filemanager.data.local.CloudUploadEntity

/**
 * The never-silent "what's happening with my uploads" surface Phase 16's own spec calls for --
 * queued/uploading entries show their progress, and a failed one stays visibly marked (with a
 * Retry and a Dismiss action) until the user actually does something about it, rather than
 * quietly disappearing. Shown above Browse's file list whenever there's at least one entry worth
 * showing (see `uploadsToDisplay` -- finished uploads drop off on their own).
 */
@Composable
internal fun UploadStatusBanner(
    uploads: List<CloudUploadEntity>,
    onRetry: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.uploads_section_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn {
            items(uploads, key = { it.id }) { upload -> UploadStatusRow(upload, onRetry, onDismiss) }
        }
    }
}

@Composable
private fun UploadStatusRow(
    upload: CloudUploadEntity,
    onRetry: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
) {
    val status = runCatching { CloudUploadStatus.valueOf(upload.status) }.getOrDefault(CloudUploadStatus.FAILED)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(upload.sourceName, style = MaterialTheme.typography.bodyMedium)
            val statusColor =
                if (status == CloudUploadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            Text(text = stringResource(status.labelRes()), style = MaterialTheme.typography.bodySmall, color = statusColor)
        }
        if (status == CloudUploadStatus.FAILED) {
            TextButton(onClick = { onRetry(upload.id) }) { Text(stringResource(R.string.upload_retry_action)) }
            TextButton(onClick = { onDismiss(upload.id) }) { Text(stringResource(R.string.upload_dismiss_action)) }
        }
    }
}
