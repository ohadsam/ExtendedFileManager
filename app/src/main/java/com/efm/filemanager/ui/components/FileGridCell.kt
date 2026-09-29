package com.efm.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.efm.filemanager.domain.model.FileEntry

private val GRID_THUMBNAIL_SIZE = 64.dp

/** A single file/folder cell for grid view mode -- shares [FileThumbnail] with [FileRow]. */
@Composable
fun FileGridCell(
    entry: FileEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .padding(4.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(8.dp),
    ) {
        FileThumbnail(entry, size = GRID_THUMBNAIL_SIZE)
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
