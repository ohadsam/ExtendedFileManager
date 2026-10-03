package com.efm.filemanager.ui.feature.browse

internal data class SelectionBarActions(
    val onClose: () -> Unit,
    val onRename: () -> Unit,
    val onMove: () -> Unit,
    val onCopy: () -> Unit,
    val onDelete: () -> Unit,
    val onShowDetails: () -> Unit,
    val onAddTag: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onToggleLock: () -> Unit,
    val onAddToVault: () -> Unit,
    val onShare: () -> Unit,
)
