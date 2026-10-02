package com.efm.filemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 12's Timber-backed diagnostic log -- distinct from [com.efm.filemanager.data.audit.AuditEventEntity],
 * which is Phase 13's user-action trail. [priority] mirrors `android.util.Log`'s levels (the same
 * ints Timber itself uses), so a release build without a Logcat a user can hand over still has a
 * diagnostic trail the Phase 12 viewer can show and export.
 */
@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val priority: Int,
    val tag: String?,
    val message: String,
    val stackTrace: String?,
)
