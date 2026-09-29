package com.efm.filemanager.data.advisor

import com.efm.filemanager.domain.model.RecommendationReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val DAY_MILLIS = 1000L * 60 * 60 * 24
private const val THRESHOLD = DAY_MILLIS * 30

class StorageAdvisorHeuristicsTest {
    @Test
    fun `matches known temp extensions case-insensitively`() {
        assertEquals(".tmp", matchesTemporaryPattern("download.TMP"))
        assertEquals(".log", matchesTemporaryPattern("app.log"))
        assertNull(matchesTemporaryPattern("photo.jpg"))
    }

    @Test
    fun `matches on the real, final extension -- a non-temp extension after a temp-like segment is not a match`() {
        assertNull(matchesTemporaryPattern("notes.tmp.pdf"))
        assertEquals(".tmp", matchesTemporaryPattern("report.v2.tmp"))
    }

    @Test
    fun `matches a leftover Android trash rename by prefix`() {
        assertEquals(".trashed-*", matchesTemporaryPattern(".trashed-1690000000-photo.jpg"))
    }

    @Test
    fun `flags an Android media folder whose package is no longer installed`() {
        val isOrphaned =
            isOrphanedAppMediaFolder(
                ancestorNames = listOf("primary", "Android", "media"),
                folderName = "com.uninstalled.app",
                isPackageInstalled = { it == "com.installed.app" },
            )
        assertEquals(true, isOrphaned)
    }

    @Test
    fun `does not flag a folder outside Android-media, or one whose package is still installed`() {
        assertEquals(
            false,
            isOrphanedAppMediaFolder(listOf("primary", "Download"), "com.some.app") { false },
        )
        assertEquals(
            false,
            isOrphanedAppMediaFolder(listOf("primary", "Android", "media"), "com.installed.app") { it == "com.installed.app" },
        )
    }

    private val thresholds = LargeFileThresholds(minSizeBytes = 100, unusedThresholdMillis = THRESHOLD)

    @Test
    fun `large-unused ignores files under the size floor`() {
        val reason = largeUnusedReason(sizeBytes = 10, lastModified = 0, lastOpenedAt = null, now = THRESHOLD * 10, thresholds = thresholds)
        assertNull(reason)
    }

    @Test
    fun `large-unused falls back to lastModified when never opened via EFM`() {
        val old =
            largeUnusedReason(sizeBytes = 1_000, lastModified = 0, lastOpenedAt = null, now = THRESHOLD + 1, thresholds = thresholds)
        assertEquals(RecommendationReason.NOT_MODIFIED_RECENTLY, old)

        val recent =
            largeUnusedReason(
                sizeBytes = 1_000,
                lastModified = THRESHOLD,
                lastOpenedAt = null,
                now = THRESHOLD + 1,
                thresholds = thresholds,
            )
        assertNull(recent)
    }

    @Test
    fun `a recent EFM open overrides a stale lastModified`() {
        val reason =
            largeUnusedReason(
                sizeBytes = 1_000,
                lastModified = 0,
                lastOpenedAt = THRESHOLD,
                now = THRESHOLD + 1,
                thresholds = thresholds,
            )
        assertNull(reason)
    }

    @Test
    fun `a stale EFM open is flagged as not-opened-via-app, even with a stale lastModified too`() {
        val reason =
            largeUnusedReason(sizeBytes = 1_000, lastModified = 0, lastOpenedAt = 0, now = THRESHOLD + 1, thresholds = thresholds)
        assertEquals(RecommendationReason.NOT_OPENED_VIA_APP, reason)
    }
}
