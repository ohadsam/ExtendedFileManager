package com.efm.filemanager.data.logs

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Forwards every Timber call into [LogRepository]'s Room-backed store, so a release build --
 * which has no Logcat a user can hand over -- still has a diagnostic trail the Phase 12 viewer
 * can show and export. VERBOSE/DEBUG are skipped: at that volume they'd blow past anything the
 * weekly purge worker can keep bounded without being useful enough to justify it.
 */
class RoomLogTree
    @Inject
    constructor(
        private val logRepository: LogRepository,
    ) : Timber.Tree() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Widened to public (Timber's own isLoggable is protected) so RoomLogTreeTest can
        // exercise the filtering rule directly instead of only through planted-Tree behavior.
        public override fun isLoggable(
            tag: String?,
            priority: Int,
        ): Boolean = priority >= Log.INFO

        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {
            scope.launch {
                logRepository.record(priority, tag, message, t?.stackTraceToString())
            }
        }
    }
