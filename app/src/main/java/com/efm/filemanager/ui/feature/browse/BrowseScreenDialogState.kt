package com.efm.filemanager.ui.feature.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class BrowseScreenDialogState {
    var dialog by mutableStateOf<BrowseDialog?>(null)
    var archiveRequest by mutableStateOf<ArchiveDialogRequest?>(null)
}
