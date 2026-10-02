package com.efm.filemanager.data.logs

import com.efm.filemanager.data.local.LogEntryDao
import com.efm.filemanager.data.local.LogEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogRepository
    @Inject
    constructor(
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
    }
