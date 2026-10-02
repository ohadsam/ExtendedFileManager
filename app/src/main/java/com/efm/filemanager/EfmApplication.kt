package com.efm.filemanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.efm.filemanager.data.logs.RoomLogTree
import com.efm.filemanager.data.metadata.TagRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point. Implements [Configuration.Provider] so WorkManager builds its
 * Workers (e.g. Phase 6's DuplicateScanWorker) through Hilt instead of a no-arg constructor --
 * WorkManager's own default initializer is disabled in AndroidManifest.xml in favor of this.
 * Also seeds Phase 9's out-of-the-box tags once, the one piece of startup work small and
 * idempotent enough not to need a WorkManager job of its own, and plants Phase 12's
 * [RoomLogTree] so every `Timber` call anywhere in the app lands in the Room-backed log store
 * from the very first line of startup.
 */
@HiltAndroidApp
class EfmApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var tagRepository: TagRepository

    @Inject
    lateinit var roomLogTree: RoomLogTree

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Timber.plant(roomLogTree)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { tagRepository.ensureDefaultTagsSeeded() }
    }
}
