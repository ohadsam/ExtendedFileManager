package com.efm.filemanager.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.efm.filemanager.R

/** The drawer's entries grow one at a time as each owning phase lands (see docs/PLAN.md's "Navigation & toolbar conventions"). */
enum class EfmDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    // Phase 19's final slice: now the app's start destination, so it leads the drawer too --
    // the drawer's own top-to-bottom order always matches what a cold start lands on first.
    Dashboard("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard),

    Browse("browse", R.string.nav_browse, Icons.Filled.Folder),
    Duplicates("duplicates", R.string.nav_duplicates, Icons.Filled.FileCopy),
    Favorites("favorites", R.string.nav_favorites, Icons.Filled.Star),
    Insights("insights", R.string.nav_insights, Icons.Filled.Insights),
    Statistics("statistics", R.string.nav_statistics, Icons.Filled.BarChart),
    Vault("vault", R.string.nav_vault, Icons.Filled.Security),
    Logs("logs", R.string.nav_logs, Icons.Filled.ReceiptLong),
    Audit("audit", R.string.nav_audit, Icons.Filled.History),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings),
}
