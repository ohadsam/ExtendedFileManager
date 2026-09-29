package com.efm.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.efm.filemanager.domain.model.TagColor

/** A row of tappable swatches, one per [TagColor] -- the color half of creating/editing a tag. */
@Composable
fun TagColorPicker(
    selected: TagColor,
    onSelect: (TagColor) -> Unit,
) {
    Row {
        TagColor.entries.forEach { color ->
            val borderColor = if (color == selected) MaterialTheme.colorScheme.primary else color.containerColor()
            Box(
                modifier =
                    Modifier
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.containerColor())
                        .border(2.dp, borderColor, CircleShape)
                        .clickable { onSelect(color) },
            )
        }
    }
}
