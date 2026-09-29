package com.efm.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.ui.feature.browse.formatDate
import com.efm.filemanager.ui.feature.browse.formatFileSize

enum class FileRowDensity { COMPACT, NORMAL, DETAILED }

/** A single file/folder row -- shared by any screen that lists [FileEntry]s (browse, search, ...). */
@Composable
fun FileRow(
    entry: FileEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    density: FileRowDensity = FileRowDensity.NORMAL,
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val (thumbnailSize, verticalPadding) = rowMetrics(density)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(end = 16.dp)) {
            FileThumbnail(entry, size = thumbnailSize)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = density.nameStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = fileMetaLabel(entry),
                style = density.metaStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (density == FileRowDensity.DETAILED) {
                Text(
                    text = entry.sourceApp?.packageName ?: stringResource(R.string.source_app_unknown),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun rowMetrics(density: FileRowDensity): Pair<Dp, Dp> =
    when (density) {
        FileRowDensity.COMPACT -> COMPACT_THUMBNAIL_SIZE to COMPACT_VERTICAL_PADDING
        FileRowDensity.NORMAL -> NORMAL_THUMBNAIL_SIZE to NORMAL_VERTICAL_PADDING
        FileRowDensity.DETAILED -> DETAILED_THUMBNAIL_SIZE to DETAILED_VERTICAL_PADDING
    }

@Composable
private fun FileRowDensity.nameStyle() =
    if (this == FileRowDensity.COMPACT) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge

@Composable
private fun FileRowDensity.metaStyle() =
    if (this == FileRowDensity.COMPACT) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall

private val COMPACT_THUMBNAIL_SIZE = 28.dp
private val NORMAL_THUMBNAIL_SIZE = 40.dp
private val DETAILED_THUMBNAIL_SIZE = 52.dp
private val COMPACT_VERTICAL_PADDING = 6.dp
private val NORMAL_VERTICAL_PADDING = 12.dp
private val DETAILED_VERTICAL_PADDING = 16.dp

private fun fileMetaLabel(entry: FileEntry): String {
    val dateLabel = formatDate(entry.lastModified)
    if (entry.isDirectory) return dateLabel
    return "${formatFileSize(entry.size)} • $dateLabel"
}
