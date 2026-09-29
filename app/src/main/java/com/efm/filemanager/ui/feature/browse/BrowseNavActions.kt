package com.efm.filemanager.ui.feature.browse

/** [BrowseScreen]'s cross-screen navigation callbacks, bundled to keep that composable's own arity down. */
data class BrowseNavActions(
    val onOpenDrawer: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenPreview: () -> Unit,
    val onOpenManageTags: () -> Unit,
)
