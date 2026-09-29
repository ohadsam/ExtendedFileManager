package com.efm.filemanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.ui.graphics.vector.ImageVector
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.ViewMode

/** Shared label/icon mapping for [ViewMode] -- used by both Settings' Display section and Browse/Search's quick toggle. */
internal fun ViewMode.labelRes(): Int =
    when (this) {
        ViewMode.LIST -> R.string.view_mode_list
        ViewMode.COMPACT -> R.string.view_mode_compact
        ViewMode.DETAILED -> R.string.view_mode_detailed
        ViewMode.GRID -> R.string.view_mode_grid
    }

internal fun ViewMode.icon(): ImageVector =
    when (this) {
        ViewMode.LIST -> Icons.Filled.ViewList
        ViewMode.COMPACT -> Icons.Filled.ViewCompact
        ViewMode.DETAILED -> Icons.Filled.ViewAgenda
        ViewMode.GRID -> Icons.Filled.GridView
    }
