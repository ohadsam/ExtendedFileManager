package com.efm.filemanager.ui.feature.search

import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.QuerySpec

data class SearchUiState(
    val isIndexing: Boolean = false,
    val hasSearched: Boolean = false,
    val results: List<FileEntry> = emptyList(),
    val querySpec: QuerySpec = QuerySpec(),
)
