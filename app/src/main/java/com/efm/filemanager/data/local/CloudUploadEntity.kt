package com.efm.filemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One queued/in-flight/finished upload (Phase 16) -- [status] is a [com.efm.filemanager.data.cloud.CloudUploadStatus]
 * name, stored as a plain string the same way [com.efm.filemanager.data.audit.AuditEventEntity] stores its action.
 */
@Entity(tableName = "cloud_uploads")
data class CloudUploadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceUri: String,
    val sourceName: String,
    val destinationParentUri: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val errorMessage: String? = null,
)
