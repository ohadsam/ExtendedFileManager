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
 * [com.efm.filemanager.data.advisor.StorageAdvisorRepository.scan] -- the duplicate count is the
 * one exception: it only reads whatever's already cached when nothing's changed since the last
 * time this worker actually ran [com.efm.filemanager.data.duplicates.DuplicateScanRepository.scan]
 * itself (same `changeVersion`-comparison skip [com.efm.filemanager.data.statistics.StatisticsRepository]
 * already uses), and re-runs that real, expensive scan otherwise -- so the notification's
 * duplicate count can't go stale indefinitely the way it could before this landed. Settings'
 * "Run daily insights" toggle skips the insights scan/notification specifically; Phase 18's daily
 * storage-stats snapshot (below) piggybacks on this same cadence regardless of that toggle, since
 * it's a separate dashboard feature, not part of Insights. Also folded into the same scan:
 * Phase 10's own "notify once a staged item is due for review" -- there's no separate
 * notification for it, same "one summary notification, never one per category" shape as
 * everything else this worker already counts.
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
            val duplicateCount = duplicateCount()
            val overdueStagedCount =
                repositories.fileFlagsRepository.countOverdueStaged(preferencesRepository.stagedReviewDays.first())
            val foundCount = advisorCount + duplicateCount + overdueStagedCount
            Timber.i(
                "DailyInsightsWorker: found %d total (%d advisor + %d duplicate + %d overdue staged)",
                foundCount,
                advisorCount,
                duplicateCount,
                overdueStagedCount,
            )
            if (preferencesRepository.notifyMeEnabled.first()) {
                insightsNotifier.notifySummary(foundCount)
            } else {
                Timber.i("DailyInsightsWorker: notify-me disabled via Settings, skipping notification")
            }
        }

        /** Re-runs the real duplicate scan only when the file index has moved since the last time this ran it. */
        private suspend fun duplicateCount(): Int {
            val currentVersion = preferencesRepository.changeVersion.first()
            val lastScannedVersion = preferencesRepository.lastDuplicateScanChangeVersion.first()
            if (lastScannedVersion == currentVersion) {
                Timber.i("DailyInsightsWorker: file index unchanged, reusing cached duplicate count")
                return repositories.duplicateScanRepository.observeGroups().first().sumOf { it.files.size }
            }
            Timber.i("DailyInsightsWorker: file index changed, re-running duplicate scan")
            val count = repositories.duplicateScanRepository.scan {}
            preferencesRepository.setLastDuplicateScanChangeVersion(currentVersion)
            return count
        }
    }
