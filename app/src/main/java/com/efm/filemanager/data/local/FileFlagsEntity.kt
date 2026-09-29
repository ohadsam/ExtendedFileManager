package com.efm.filemanager.data.local

import androidx.room.Entity

@Entity(tableName = "file_flags", primaryKeys = ["fileUri"])
data class FileFlagsEntity(
    val fileUri: String,
    val locked: Boolean = false,
    val note: String? = null,
    /** Set by Phase 10's staged-for-deletion review ("mark now, decide later"); null means not staged. */
    val stagedAt: Long? = null,
    /** Set whenever the user previews/opens this file through EFM itself (Phase 7) -- Phase 10's secondary "unused" signal. */
    val lastOpenedAt: Long? = null,
)
