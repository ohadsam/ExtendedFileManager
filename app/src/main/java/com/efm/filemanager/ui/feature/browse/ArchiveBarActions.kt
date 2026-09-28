package com.efm.filemanager.ui.feature.browse

internal data class ArchiveBarActions(
    val onCompress: () -> Unit,
    val onExtract: () -> Unit,
    val onExtractAndReplace: () -> Unit,
)
