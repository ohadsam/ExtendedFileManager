package com.efm.filemanager.data.audit

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Groundwork for Phase 13's full audit-trail viewer -- every mutating file operation
 * (Phase 3 onward) writes one of these, whether it succeeded or failed, so the trail
 * is complete from day one instead of retrofitted once the viewer exists.
 */
@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val action: String,
    val targetName: String,
    val targetUri: String,
    val detail: String?,
    val success: Boolean,
)
