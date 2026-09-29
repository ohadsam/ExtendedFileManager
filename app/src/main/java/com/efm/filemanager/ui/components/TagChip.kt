package com.efm.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.efm.filemanager.domain.model.FileTag

/** A small colored pill for one [FileTag] -- the same look everywhere a tag is shown. */
@Composable
fun TagChip(tag: FileTag) {
    Text(
        text = tag.name,
        style = MaterialTheme.typography.labelSmall,
        color = tag.color.onContainerColor(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(tag.color.containerColor())
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
