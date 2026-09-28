package com.efm.filemanager.ui.feature.browse

import com.efm.filemanager.domain.model.QuerySpec

internal data class BrowseTopBarActions(
    val onOpenDrawer: () -> Unit,
    val onNavigateToBreadcrumb: (Int) -> Unit,
    val onQuerySpecChanged: (QuerySpec) -> Unit,
    val onOpenSearch: () -> Unit,
)
