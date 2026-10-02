package com.efm.filemanager.data.audit

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Read side of the audit trail -- writing a new entry happens through [AuditLogger], already wired into every mutating repository. */
class AuditRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val auditEventDao: AuditEventDao,
    ) {
        fun observeAll(): Flow<List<AuditEventEntity>> = auditEventDao.observeAll()

        /** Writes [text] to an already-granted destination, such as one from a `CreateDocument` picker. */
        suspend fun exportText(
            uri: Uri,
            text: String,
        ) = withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }
    }
