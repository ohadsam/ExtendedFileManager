package com.efm.filemanager.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.vector.ImageVector
import com.efm.filemanager.R

/**
 * The drawer's entries grow one at a time as each owning phase lands (see docs/PLAN.md's
 * "Navigation & toolbar conventions") -- Browse is the only real destination until Phase 2
 * onward add their own.
 */
enum class EfmDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Browse("browse", R.string.nav_browse, Icons.Filled.Folder),
}
