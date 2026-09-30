package com.efm.filemanager.data.local

import com.efm.filemanager.domain.model.VaultEntry

fun VaultEntryEntity.toDomain(): VaultEntry =
    VaultEntry(
        id = id,
        name = originalName,
        mimeType = mimeType,
        size = size,
        addedAt = addedAt,
    )
