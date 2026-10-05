package com.efm.filemanager.domain.model

/**
 * One row of Phase 18's most-populated-folders widget: [name] is just the folder's own display
 * name (not a full breadcrumb path), so two same-named folders in different places show the same
 * label -- showing the full path would need walking each folder's ancestor chain, which this
 * widget doesn't do.
 */
data class FolderFileCount(
    val name: String,
    val fileCount: Int,
)
