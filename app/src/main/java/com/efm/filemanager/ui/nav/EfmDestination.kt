package com.efm.filemanager.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.efm.filemanager.R

/**
 * The drawer's entries grow one at a time as each owning phase lands (see docs/PLAN.md's
 * "Navigation & toolbar conventions") -- Favorites/Duplicates/Storage Advisor/Logs/Audit
 * are added by their own owning phases.
 */
enum class EfmDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Browse("browse", R.string.nav_browse, Icons.Filled.Folder),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings),
}
