package com.efm.filemanager.ui.components

import com.efm.filemanager.domain.model.FileEntry

/** Bundles a grouped file list's two callbacks so [GroupedFileList] stays within the app's function-arity convention. */
data class FileEntryActions(
    val onClick: (FileEntry) -> Unit,
    val onLongClick: (FileEntry) -> Unit,
)
