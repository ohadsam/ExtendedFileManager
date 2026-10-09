package com.efm.filemanager.data.insights

import com.efm.filemanager.data.advisor.StorageAdvisorRepository
import com.efm.filemanager.data.duplicates.DuplicateScanRepository
import com.efm.filemanager.data.metadata.FileFlagsRepository
import com.efm.filemanager.data.statistics.StatisticsRepository
import javax.inject.Inject

/** Bundles [DailyInsightsWorker]'s repositories so its own constructor stays under detekt's `LongParameterList` threshold. */
class DailyInsightsRepositories
    @Inject
    constructor(
        val storageAdvisorRepository: StorageAdvisorRepository,
        val duplicateScanRepository: DuplicateScanRepository,
        val statisticsRepository: StatisticsRepository,
        val fileFlagsRepository: FileFlagsRepository,
    )
