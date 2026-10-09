package com.efm.filemanager.data.statistics

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val BYTES_PER_MB = 1024L * 1024L

/**
 * Phase 19's low-storage Dashboard widget needs the device's own free space, which nothing else
 * in this app tracks -- [StatisticsRepository]'s `StorageStats`/`storage_snapshots` only ever
 * cover SAF-granted trees' file sizes, never the underlying volume's real capacity. `StatFs` on
 * the app's own files directory is the simplest correct source for that: it lives on the same
 * internal-storage partition free space actually matters for, with no extra permission needed.
 */
class DeviceStorageRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun freeBytes(): Long = StatFs(context.filesDir.path).availableBytes
    }

internal fun isLowStorage(
    freeBytes: Long,
    thresholdMb: Int,
): Boolean = freeBytes < thresholdMb * BYTES_PER_MB
