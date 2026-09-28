package com.efm.filemanager.data.documenttree

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry

/** A granted tree root plus the chain of folders from that root down to (and including) a location. */
data class LocationChain(
    val rootUri: Uri,
    val folders: List<FileEntry>,
)
