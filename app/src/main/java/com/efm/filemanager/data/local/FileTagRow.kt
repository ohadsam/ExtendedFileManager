package com.efm.filemanager.data.local

/** One file-to-tag join row, projected straight from [TagDao.observeFileTagRows]'s query. */
data class FileTagRow(
    val fileUri: String,
    val tagId: Long,
    val name: String,
    val color: String,
    val pinned: Boolean,
)
