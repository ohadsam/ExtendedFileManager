package com.efm.filemanager.ui.feature.browse

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry

internal data class DialogsContext(
    val selectedEntries: List<FileEntry>,
    val pickerState: PickerUiState,
    val parentUri: Uri?,
)
