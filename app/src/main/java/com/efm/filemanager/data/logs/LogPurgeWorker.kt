package com.efm.filemanager.data.logs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

internal const val LOG_PURGE_WORK_NAME = "log_purge"
internal val LOG_RETENTION_MILLIS = TimeUnit.DAYS.toMillis(7)

/**
 * Weekly, so a long-running install's log store never grows unbounded -- the Phase 12 viewer
 * is meant to show a recent diagnostic trail, not every entry since install.
 */
@HiltWorker
class LogPurgeWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val logRepository: LogRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            logRepository.purgeOlderThan(System.currentTimeMillis() - LOG_RETENTION_MILLIS)
            return Result.success()
        }
    }
