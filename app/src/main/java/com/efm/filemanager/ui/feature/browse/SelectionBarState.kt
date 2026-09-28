package com.efm.filemanager.ui.feature.browse

internal data class SelectionBarState(
    val selectedCount: Int,
    val canExtract: Boolean,
    val actions: SelectionBarActions,
    val archiveActions: ArchiveBarActions,
)
