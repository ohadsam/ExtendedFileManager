package com.efm.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.previewType
import com.efm.filemanager.ui.feature.browse.formatDate
import com.efm.filemanager.ui.feature.browse.formatFileSize

private val THUMBNAIL_SIZE = 40.dp

/** A single file/folder row -- shared by any screen that lists [FileEntry]s (browse, search, ...). */
@Composable
fun FileRow(
    entry: FileEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileRowLeadingIcon(entry)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = fileMetaLabel(entry),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** An actual image thumbnail for image files -- the list's "inline preview" -- a generic icon for everything else. */
@Composable
private fun FileRowLeadingIcon(entry: FileEntry) {
    Box(modifier = Modifier.padding(end = 16.dp)) {
        if (entry.previewType() == PreviewType.IMAGE) {
            AsyncImage(
                model = entry.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(THUMBNAIL_SIZE).clip(RoundedCornerShape(4.dp)),
            )
        } else {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                contentDescription = null,
            )
        }
    }
}

private fun fileMetaLabel(entry: FileEntry): String {
    val dateLabel = formatDate(entry.lastModified)
    if (entry.isDirectory) return dateLabel
    return "${formatFileSize(entry.size)} • $dateLabel"
}
