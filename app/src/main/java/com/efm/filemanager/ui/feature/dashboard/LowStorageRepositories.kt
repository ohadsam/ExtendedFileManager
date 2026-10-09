package com.efm.filemanager.ui.feature.dashboard

import com.efm.filemanager.data.prefs.PreferencesRepository
import com.efm.filemanager.data.statistics.DeviceStorageRepository
import javax.inject.Inject

/** Bundles [DashboardViewModel]'s low-storage-widget dependencies so its own constructor stays under detekt's `LongParameterList` threshold. */
class LowStorageRepositories
    @Inject
    constructor(
        val deviceStorageRepository: DeviceStorageRepository,
        val preferencesRepository: PreferencesRepository,
    )
