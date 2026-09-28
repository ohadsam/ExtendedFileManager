package com.efm.filemanager.domain.model

data class FileGroup(
    val key: GroupKey,
    val files: List<FileEntry>,
)
