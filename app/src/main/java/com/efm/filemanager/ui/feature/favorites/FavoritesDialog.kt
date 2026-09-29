package com.efm.filemanager.ui.feature.favorites

import com.efm.filemanager.domain.model.FavoriteCollection

internal sealed interface FavoritesDialog {
    data class CreateCollection(val parentId: Long?) : FavoritesDialog

    data class Rename(val collection: FavoriteCollection) : FavoritesDialog

    data class Delete(val collection: FavoriteCollection) : FavoritesDialog
}
