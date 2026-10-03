package com.efm.filemanager.ui.feature.browse

import com.efm.filemanager.data.cloud.CloudUploadRepository
import com.efm.filemanager.data.vault.VaultRepository
import com.efm.filemanager.ui.feature.preview.PreviewSessionHolder
import javax.inject.Inject

/**
 * Bundles three singleton, single-purpose delegates -- starting a Preview session, adding a
 * file to the vault, and tracking cloud uploads -- behind one constructor parameter, purely to
 * keep [BrowseViewModel]'s own parameter count under detekt's `LongParameterList` threshold.
 * There's no deeper conceptual link between the three beyond each being "hand this single
 * action off to another feature" dependencies Browse triggers but doesn't own -- same technique
 * docs/PLAN.md Phase 10 already used for `AdvisorScanSettings`.
 */
class BrowseDelegateSupport
    @Inject
    constructor(
        val previewSessionHolder: PreviewSessionHolder,
        val vaultRepository: VaultRepository,
        val cloudUploadRepository: CloudUploadRepository,
    )
