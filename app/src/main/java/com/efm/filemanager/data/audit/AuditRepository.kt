package com.efm.filemanager.data.audit

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Read side of the audit trail -- writing happens through [AuditLogger], already wired into every mutating repository. */
class AuditRepository
    @Inject
    constructor(
        private val auditEventDao: AuditEventDao,
    ) {
        fun observeAll(): Flow<List<AuditEventEntity>> = auditEventDao.observeAll()
    }
