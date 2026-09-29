package com.efm.filemanager.data.advisor

import com.efm.filemanager.data.metadata.FileFlagsRepository
import com.efm.filemanager.data.prefs.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The two per-scan inputs [StorageAdvisorRepository.scan] needs from elsewhere -- bundled into
 * one injected dependency so that repository's own constructor doesn't grow past detekt's
 * LongParameterList threshold as Phase 10 adds more Settings-tunable behavior.
 */
class AdvisorScanSettings
    @Inject
    constructor(
        private val fileFlagsRepository: FileFlagsRepository,
        private val preferencesRepository: PreferencesRepository,
    ) {
        suspend fun lastOpenedAtByUri(): Map<String, Long> = fileFlagsRepository.lastOpenedAtByUriOnce()

        suspend fun largeFileThresholds(): LargeFileThresholds =
            thresholdsFrom(preferencesRepository.advisorMinSizeMb.first(), preferencesRepository.advisorUnusedMonths.first())
    }
