package com.efm.filemanager.data.trash

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One soft-deleted file/folder, tracked so an Undo snackbar (Phase 3) can move it back
 * to where it came from. There's no user-facing "Recently Deleted" browser yet -- that's
 * a future extra on top of this -- so an entry only ever leaves this table via undo.
 */
@Entity(tableName = "trashed_files")
data class TrashedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trashUri: String,
    val originalParentUri: String,
    val originalName: String,
    val trashedAt: Long,
)
