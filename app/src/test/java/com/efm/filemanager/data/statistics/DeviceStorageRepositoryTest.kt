package com.efm.filemanager.data.statistics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceStorageRepositoryTest {
    @Test
    fun `free space below the threshold is low storage`() {
        assertTrue(isLowStorage(freeBytes = 500L * 1024 * 1024, thresholdMb = 1_000))
    }

    @Test
    fun `free space at or above the threshold is not low storage`() {
        assertFalse(isLowStorage(freeBytes = 1_000L * 1024 * 1024, thresholdMb = 1_000))
        assertFalse(isLowStorage(freeBytes = 2_000L * 1024 * 1024, thresholdMb = 1_000))
    }
}
