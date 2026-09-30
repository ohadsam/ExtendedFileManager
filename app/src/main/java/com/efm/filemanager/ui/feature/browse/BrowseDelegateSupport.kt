package com.efm.filemanager.ui.feature.browse

import com.efm.filemanager.data.vault.VaultRepository
import com.efm.filemanager.ui.feature.preview.PreviewSessionHolder
import javax.inject.Inject

/**
 * Bundles two singleton, single-purpose delegates -- starting a Preview session and adding a
 * file to the vault -- behind one constructor parameter, purely to keep [BrowseViewModel]'s own
 * parameter count under detekt's `LongParameterList` threshold. There's no deeper conceptual
 * link between the two beyond both being "hand this single action off to another feature"
 * dependencies Browse triggers but doesn't own -- same technique docs/PLAN.md Phase 10 already
 * used for `AdvisorScanSettings`.
 */
class BrowseDelegateSupport
    @Inject
    constructor(
        val previewSessionHolder: PreviewSessionHolder,
        val vaultRepository: VaultRepository,
    )
