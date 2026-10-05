package com.efm.filemanager.ui.feature.globalfiles

import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.GlobalFilesSort

data class GlobalFileListUiState(
    val isLoading: Boolean = true,
    val files: List<FileEntry> = emptyList(),
    val category: FileCategory? = null,
    val sort: GlobalFilesSort = GlobalFilesSort.SIZE,
)
