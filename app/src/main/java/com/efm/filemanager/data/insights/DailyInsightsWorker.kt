package com.efm.filemanager.data.insights

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.efm.filemanager.data.advisor.StorageAdvisorRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

internal const val DAILY_INSIGHTS_WORK_NAME = "daily_insights"

/**
 * Phase 17's proactive habit: once a day, re-run Phase 10's storage-advisor scan in the
 * background and -- if it found anything worth a look -- hand the count to [InsightsNotifier]
 * for one summary notification, rather than leaving the Advisor screen as something the user
 * has to remember to open. Duplicate-folding and the changeVersion-based incremental skip for
 * the expensive duplicate scan are still open (see docs/PLAN.md Phase 17) -- this worker only
 * recomputes Phase 10's already-cheap recommendation categories, the same ones the Advisor
 * screen already shows and already persists to Room via [StorageAdvisorRepository.scan].
 */
@HiltWorker
class DailyInsightsWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val storageAdvisorRepository: StorageAdvisorRepository,
        private val insightsNotifier: InsightsNotifier,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            Timber.i("DailyInsightsWorker: starting daily scan")
            val foundCount = storageAdvisorRepository.scan {}
            Timber.i("DailyInsightsWorker: scan found %d recommendation(s)", foundCount)
            insightsNotifier.notifySummary(foundCount)
            return Result.success()
        }
    }
