package com.efm.filemanager.data.vault

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.metadata.FileFlagsRepository
import javax.inject.Inject

/**
 * Bundles the two simple, narrowly-used lookups [VaultRepository] needs before touching a file
 * -- whether it's locked, and its parent uri when not already known (e.g. an entry reached from
 * a screen other than Browse, which doesn't track a single "current folder") -- behind one
 * constructor parameter, purely to keep VaultRepository's own parameter count under detekt's
 * `LongParameterList` threshold. Same technique as `BrowseDelegateSupport`/`AdvisorScanSettings`.
 */
class VaultEntryLookupSupport
    @Inject
    constructor(
        private val fileFlagsRepository: FileFlagsRepository,
        private val documentTreeRepository: DocumentTreeRepository,
    ) {
        suspend fun isLocked(uri: Uri): Boolean = fileFlagsRepository.isLocked(uri)

        suspend fun parentUriOf(uri: Uri): Uri? = documentTreeRepository.parentUriOf(uri)
    }
