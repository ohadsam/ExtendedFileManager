package com.efm.filemanager.domain.model

/** A node in the self-referencing favorites tree -- [parentId] null means a top-level collection. */
data class FavoriteCollection(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val sortOrder: Int,
)
