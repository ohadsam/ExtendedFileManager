package com.efm.filemanager.ui.feature.browse

import com.efm.filemanager.domain.model.QuerySpec

internal data class BrowseTopBarState(
    val breadcrumbs: List<BreadcrumbEntry>,
    val selectionBarState: SelectionBarState,
    val querySpec: QuerySpec,
)
