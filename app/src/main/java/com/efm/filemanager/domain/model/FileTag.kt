package com.efm.filemanager.domain.model

/** A catalog entry: defined once, then applied many-to-many to files/folders -- see docs/PLAN.md Phase 9. */
data class FileTag(
    val id: Long,
    val name: String,
    val color: TagColor,
    val pinned: Boolean,
)
