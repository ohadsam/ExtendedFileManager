package com.efm.filemanager.domain.model

data class DuplicateGroup(
    val hash: String,
    val fileSize: Long,
    val files: List<FileEntry>,
)
