package com.efm.filemanager.ui.feature.browse

internal sealed interface ArchiveDialogRequest {
    data object Compress : ArchiveDialogRequest

    data class Extract(val request: ExtractRequest) : ArchiveDialogRequest
}
