package com.efm.filemanager.ui.feature.preview

import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.previewType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PreviewSession(
    val entries: List<FileEntry>,
    val startIndex: Int,
)

/** Narrows [allEntries] to the previewable ones and locates [tapped] among them, or null if it has no preview. */
fun buildPreviewSession(
    allEntries: List<FileEntry>,
    tapped: FileEntry,
): PreviewSession? {
    val previewable = allEntries.filter { entry -> entry.previewType() != PreviewType.NONE }
    val index = previewable.indexOfFirst { entry -> entry.uri == tapped.uri }
    return if (index == -1) null else PreviewSession(previewable, index)
}

/**
 * Carries a preview request (which files, starting where) from whichever screen a file was
 * tapped in -- Browse, Search, or Duplicates -- to the Preview screen. A plain singleton
 * rather than routing the list through navigation args, since Compose Navigation only
 * serializes primitives cleanly and a FileEntry list/Uri isn't one.
 */
@Singleton
class PreviewSessionHolder
    @Inject
    constructor() {
        private val _session = MutableStateFlow<PreviewSession?>(null)
        val session: StateFlow<PreviewSession?> = _session.asStateFlow()

        fun start(
            entries: List<FileEntry>,
            startIndex: Int,
        ) {
            _session.value = PreviewSession(entries, startIndex)
        }

        fun clear() {
            _session.value = null
        }
    }
