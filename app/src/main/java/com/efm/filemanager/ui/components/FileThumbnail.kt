package com.efm.filemanager.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.previewType

/** An actual image thumbnail for image files -- a generic icon otherwise. Shared by [FileRow] and [FileGridCell]. */
@Composable
fun FileThumbnail(
    entry: FileEntry,
    size: Dp,
) {
    if (entry.previewType() == PreviewType.IMAGE) {
        AsyncImage(
            model = entry.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(RoundedCornerShape(4.dp)),
        )
    } else {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}
