package com.efm.filemanager.ui.feature.preview

internal data class PreviewSelectionActions(
    val onClose: () -> Unit,
    val onToggleCurrent: () -> Unit,
    val onAddTag: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onToggleLock: () -> Unit,
    val onShowDetails: (() -> Unit)? = null,
)
