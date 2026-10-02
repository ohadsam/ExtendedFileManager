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
            val timestamp = System.currentTimeMillis()
            val previousHash = auditEventDao.getLastHash()
            val content = AuditEventContent(timestamp, action.name, targetName, targetUri.toString(), detail, success)
            val hash = AuditHashChain.computeHash(previousHash, content)
            auditEventDao.insert(
                AuditEventEntity(
                    timestamp = timestamp,
                    action = action.name,
                    targetName = targetName,
                    targetUri = targetUri.toString(),
                    detail = detail,
                    success = success,
                    previousHash = previousHash,
                    hash = hash,
                ),
            )
        }
    }
