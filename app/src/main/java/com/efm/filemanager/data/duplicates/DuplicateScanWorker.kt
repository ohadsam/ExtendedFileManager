package com.efm.filemanager.data.duplicates

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DuplicateScanWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val repository: DuplicateScanRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val foundCount = repository.scan { progress -> setProgressAsync(progress.toWorkData()) }
            return Result.success(workDataOf(KEY_RESULT_COUNT to foundCount))
        }
    }
