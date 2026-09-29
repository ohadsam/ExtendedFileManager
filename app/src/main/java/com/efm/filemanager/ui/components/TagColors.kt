package com.efm.filemanager.ui.components

import androidx.compose.ui.graphics.Color
import com.efm.filemanager.domain.model.TagColor

/** A container/on-container pair per [TagColor] -- fixed tones so a tag looks the same in light and dark. */
internal fun TagColor.containerColor(): Color =
    when (this) {
        TagColor.RED -> Color(0xFFFFDAD6)
        TagColor.ORANGE -> Color(0xFFFFDCC2)
        TagColor.YELLOW -> Color(0xFFFFE08C)
        TagColor.GREEN -> Color(0xFFC2F0C2)
        TagColor.TEAL -> Color(0xFFB8EAE3)
        TagColor.BLUE -> Color(0xFFC6E3FF)
        TagColor.PURPLE -> Color(0xFFE3D4FF)
        TagColor.PINK -> Color(0xFFFFD6EC)
        TagColor.GRAY -> Color(0xFFE0E0E0)
    }

internal fun TagColor.onContainerColor(): Color =
    when (this) {
        TagColor.RED -> Color(0xFF410002)
        TagColor.ORANGE -> Color(0xFF4A2800)
        TagColor.YELLOW -> Color(0xFF4A3B00)
        TagColor.GREEN -> Color(0xFF0A3D0A)
        TagColor.TEAL -> Color(0xFF00352E)
        TagColor.BLUE -> Color(0xFF00325A)
        TagColor.PURPLE -> Color(0xFF2E0A5C)
        TagColor.PINK -> Color(0xFF4A0033)
        TagColor.GRAY -> Color(0xFF303030)
    }
