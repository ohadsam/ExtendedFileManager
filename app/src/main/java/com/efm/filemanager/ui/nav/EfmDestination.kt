package com.efm.filemanager.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.efm.filemanager.R

/**
 * The drawer's entries grow one at a time as each owning phase lands (see docs/PLAN.md's
 * "Navigation & toolbar conventions") -- Logs/Audit are added by their own owning phases.
 */
enum class EfmDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Browse("browse", R.string.nav_browse, Icons.Filled.Folder),
    Duplicates("duplicates", R.string.nav_duplicates, Icons.Filled.FileCopy),
    Favorites("favorites", R.string.nav_favorites, Icons.Filled.Star),
    Advisor("storage_advisor", R.string.nav_storage_advisor, Icons.Filled.CleaningServices),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings),
}
