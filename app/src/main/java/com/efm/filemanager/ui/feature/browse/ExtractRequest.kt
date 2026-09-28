package com.efm.filemanager.ui.feature.browse

import com.efm.filemanager.domain.model.FileEntry

internal data class ExtractRequest(
    val archive: FileEntry,
    val replaceOriginal: Boolean,
)
