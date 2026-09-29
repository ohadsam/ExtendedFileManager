package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag

internal data class DialogsContext(
    val selectedEntries: List<FileEntry>,
    val pickerState: PickerUiState,
    val parentUri: Uri?,
    val tags: List<FileTag>,
    val favoriteCollections: List<FavoriteCollection>,
    val onOpenManageTags: () -> Unit,
)
