package com.efm.filemanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Kept deliberately minimal in Phase 0 — structured
 * logging (Timber -> Room) lands in Phase 10, not planted speculatively here.
 */
@HiltAndroidApp
class EfmApplication : Application()
