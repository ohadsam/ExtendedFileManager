package com.efm.filemanager.data.audit

import android.net.Uri
import javax.inject.Inject

class AuditLogger
    @Inject
    constructor(
        private val auditEventDao: AuditEventDao,
    ) {
        suspend fun log(
            action: AuditAction,
            targetName: String,
            targetUri: Uri,
            success: Boolean,
            detail: String? = null,
        ) {
            auditEventDao.insert(
                AuditEventEntity(
                    timestamp = System.currentTimeMillis(),
                    action = action.name,
                    targetName = targetName,
                    targetUri = targetUri.toString(),
                    detail = detail,
                    success = success,
                ),
            )
        }
    }
