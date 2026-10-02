package com.efm.filemanager.data.logs

import android.content.Context
import android.net.Uri
import com.efm.filemanager.data.local.LogEntryDao
import com.efm.filemanager.data.local.LogEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LogRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val logEntryDao: LogEntryDao,
    ) {
        fun observeAll(): Flow<List<LogEntryEntity>> = logEntryDao.observeAll()

        suspend fun record(
            priority: Int,
            tag: String?,
            message: String,
            stackTrace: String?,
        ) {
            logEntryDao.insert(
                LogEntryEntity(
                    timestamp = System.currentTimeMillis(),
                    priority = priority,
                    tag = tag,
                    message = message,
                    stackTrace = stackTrace,
                ),
            )
        }

        suspend fun clearAll() = logEntryDao.clearAll()

        suspend fun purgeOlderThan(cutoffMillis: Long) = logEntryDao.purgeOlderThan(cutoffMillis)

        /** Writes [text] to an already-granted destination, such as one from a `CreateDocument` picker. */
        suspend fun exportText(
            uri: Uri,
            text: String,
        ) = withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }
    }
