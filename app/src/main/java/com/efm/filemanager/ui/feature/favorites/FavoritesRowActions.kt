package com.efm.filemanager.ui.feature.favorites

import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry

internal data class FavoritesRowActions(
    val onOpenCollection: (FavoriteCollection) -> Unit,
    val onMoveCollection: (FavoriteCollection, MoveDirection) -> Unit,
    val onRenameCollection: (FavoriteCollection) -> Unit,
    val onDeleteCollection: (FavoriteCollection) -> Unit,
    val onEntryClick: (FileEntry) -> Unit,
    val onEntryLongClick: (FileEntry) -> Unit,
    val onShowDetails: (FileEntry) -> Unit,
    val onAddToVault: (FileEntry) -> Unit,
)
