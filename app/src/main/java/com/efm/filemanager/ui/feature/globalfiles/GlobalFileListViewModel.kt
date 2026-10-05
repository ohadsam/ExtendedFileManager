package com.efm.filemanager.ui.feature.globalfiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.statistics.StatisticsRepository
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.GlobalFilesSort
import com.efm.filemanager.ui.feature.preview.PreviewSessionHolder
import com.efm.filemanager.ui.feature.preview.buildPreviewSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Matches the nav-arg name used for this route in `ui/EfmApp.kt` -- must stay in sync. */
internal const val GLOBAL_FILES_CATEGORY_ARG = "category"

/** Matches the nav-arg name used for this route in `ui/EfmApp.kt` -- must stay in sync. */
internal const val GLOBAL_FILES_SORT_ARG = "sort"

/** The literal nav-arg value meaning "every file, no category filter" -- `FileCategory` has no such value of its own. */
internal const val GLOBAL_FILES_ALL_CATEGORIES = "ALL"

/**
 * Phase 18's drill-down target for the by-type/largest-files/recently-modified widgets -- every
 * file across every granted tree, optionally narrowed to one [FileCategory], ordered by
 * [GlobalFilesSort] (whichever order the originating widget's own card already implies).
 */
@HiltViewModel
class GlobalFileListViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val statisticsRepository: StatisticsRepository,
        private val previewSessionHolder: PreviewSessionHolder,
    ) : ViewModel() {
        private val category: FileCategory? = savedStateHandle.get<String>(GLOBAL_FILES_CATEGORY_ARG)?.toFileCategoryArg()
        private val sort: GlobalFilesSort = savedStateHandle.get<String>(GLOBAL_FILES_SORT_ARG).toGlobalFilesSortArg()

        private val _uiState = MutableStateFlow(GlobalFileListUiState(category = category, sort = sort))
        val uiState: StateFlow<GlobalFileListUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val files = statisticsRepository.filesByCategory(category, sort)
                _uiState.value = _uiState.value.copy(isLoading = false, files = files)
            }
        }

        fun openPreview(tapped: FileEntry) {
            val session = buildPreviewSession(_uiState.value.files, tapped) ?: return
            previewSessionHolder.start(session.entries, session.startIndex)
        }
    }

internal fun String.toFileCategoryArg(): FileCategory? =
    if (this == GLOBAL_FILES_ALL_CATEGORIES) null else runCatching { FileCategory.valueOf(this) }.getOrNull()

/** A missing or unrecognized nav arg falls back to SIZE -- this route's long-standing default sort. */
internal fun String?.toGlobalFilesSortArg(): GlobalFilesSort =
    this?.let { runCatching { GlobalFilesSort.valueOf(it) }.getOrNull() } ?: GlobalFilesSort.SIZE
