package com.efm.filemanager.data.local

import androidx.room.Entity
import androidx.room.Fts4

/**
 * A lean full-text mirror of [FileEntryEntity] (uri + name only) for global search --
 * see docs/PLAN.md Phase 5. Deliberately doesn't duplicate the rest of a file's metadata:
 * a match here is resolved back to its full [FileEntryEntity] via [FileEntryDao.getByUris],
 * so [FileEntryEntity] stays the single source of truth for everything but searchability.
 */
@Fts4
@Entity(tableName = "file_search_fts")
data class FileEntryFtsEntity(
    val uri: String,
    val name: String,
)
