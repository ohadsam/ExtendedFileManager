package com.efm.filemanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Implements [Configuration.Provider] so WorkManager builds its
 * Workers (e.g. Phase 6's DuplicateScanWorker) through Hilt instead of a no-arg constructor --
 * WorkManager's own default initializer is disabled in AndroidManifest.xml in favor of this.
 */
@HiltAndroidApp
class EfmApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
