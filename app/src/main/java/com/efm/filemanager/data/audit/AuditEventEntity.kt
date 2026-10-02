package com.efm.filemanager.data.audit

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Groundwork for Phase 13's full audit-trail viewer -- every mutating file operation
 * (Phase 3 onward) writes one of these, whether it succeeded or failed, so the trail
 * is complete from day one instead of retrofitted once the viewer exists. [hash]/[previousHash]
 * hash-chain every entry together (see [AuditHashChain]), so altering or deleting a past entry
 * is detectable -- this is a security-sensitive log, distinct from Phase 12's plain debug log.
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
    val previousHash: String?,
    val hash: String,
)
