package com.efm.filemanager.ui.feature.browse

import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.ViewMode

internal data class BrowseTopBarActions(
    val onOpenDrawer: () -> Unit,
    val onNavigateToBreadcrumb: (Int) -> Unit,
    val onQuerySpecChanged: (QuerySpec) -> Unit,
    val onOpenSearch: () -> Unit,
    val onViewModeChanged: (ViewMode) -> Unit,
)
