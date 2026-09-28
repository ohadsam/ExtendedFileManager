package com.efm.filemanager.data.documenttree

/**
 * Each folder gets its own local trash subfolder the first time something is deleted
 * from it (see [FileOperationsRepository]), rather than one central bin -- that keeps
 * Undo working without inventing a cross-tree "Recently Deleted" concept before it's
 * needed. Hidden from every listing by name (see [DocumentTreeRepository]).
 */
internal const val TRASH_FOLDER_NAME = ".efm_trash"
