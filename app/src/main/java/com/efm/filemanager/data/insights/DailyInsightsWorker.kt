package com.efm.filemanager.data.insights

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.efm.filemanager.data.prefs.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

internal const val DAILY_INSIGHTS_WORK_NAME = "daily_insights"

/**
 * Phase 17's proactive habit: once a day, re-run Phase 10's storage-advisor scan in the
 * background and -- if it (plus whatever Phase 6's duplicate scan already has cached) found
 * anything worth a look -- hand the combined count to [InsightsNotifier] for one summary
 * notification, rather than leaving the Insights screen as something the user has to remember
 * to open. This worker only recomputes Phase 10's already-cheap recommendation categories, the
 * same ones the Insights screen already shows and already persists to Room via
 * [com.efm.filemanager.data.advisor.StorageAdvisorRepository.scan] -- it reads the duplicate
 * count from whatever [com.efm.filemanager.data.duplicates.DuplicateScanRepository] last cached
 * rather than re-running that expensive scan itself; the changeVersion-based incremental skip for
 * triggering a fresh duplicate scan here is still open (see docs/PLAN.md Phase 17). Settings'
 * "Run daily insights" toggle skips the insights scan/notification specifically; Phase 18's daily
 * storage-stats snapshot (below) piggybacks on this same cadence regardless of that toggle, since
 * it's a separate dashboard feature, not part of Insights.
 */
@HiltWorker
class DailyInsightsWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val repositories: DailyInsightsRepositories,
        private val insightsNotifier: InsightsNotifier,
        private val preferencesRepository: PreferencesRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            recordDailyStatsSnapshot()
            if (!preferencesRepository.runDailyInsights.first()) {
                Timber.i("DailyInsightsWorker: disabled via Settings, skipping")
                return Result.success()
            }
            runInsightsScan()
            return Result.success()
        }

        /**
         * Phase 18's trend sparkline needs one data point per day even if the user never opens
         * Statistics -- [com.efm.filemanager.data.statistics.StatisticsRepository.computeStorageStats]
         * already records today's snapshot as a side effect, and (since Phase 18's own
         * incremental cache landed) skips its full-tree walk entirely when nothing's changed
         * since the last time anything computed it.
         */
        private suspend fun recordDailyStatsSnapshot() {
            Timber.i("DailyInsightsWorker: recording today's storage-stats snapshot")
            repositories.statisticsRepository.computeStorageStats()
        }

        private suspend fun runInsightsScan() {
            Timber.i("DailyInsightsWorker: starting daily scan")
            val advisorCount = repositories.storageAdvisorRepository.scan {}
            val duplicateCount = repositories.duplicateScanRepository.observeGroups().first().sumOf { it.files.size }
            val foundCount = advisorCount + duplicateCount
            Timber.i(
                "DailyInsightsWorker: found %d total (%d advisor + %d cached duplicate)",
                foundCount,
                advisorCount,
                duplicateCount,
            )
            if (preferencesRepository.notifyMeEnabled.first()) {
                insightsNotifier.notifySummary(foundCount)
            } else {
                Timber.i("DailyInsightsWorker: notify-me disabled via Settings, skipping notification")
            }
        }
    }
