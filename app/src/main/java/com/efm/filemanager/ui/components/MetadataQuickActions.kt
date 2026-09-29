package com.efm.filemanager.ui.components

/** [MetadataQuickActionsMenu]'s actions, bundled to keep that composable's own param count down. */
data class MetadataQuickActions(
    val onAddTag: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onToggleLock: () -> Unit,
    /** Null hides the "Details" item -- only offered when exactly one file is selected. */
    val onShowDetails: (() -> Unit)? = null,
)
