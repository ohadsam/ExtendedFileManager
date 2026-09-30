package com.efm.filemanager.domain.model

/**
 * One file held in the encrypted vault -- see docs/PLAN.md Phase 11. Not a [FileEntry]: a vault
 * entry never came from (and while vaulted, no longer has) a SAF uri.
 */
data class VaultEntry(
    val id: Long,
    val name: String,
    val mimeType: String?,
    val size: Long,
    val addedAt: Long,
)
