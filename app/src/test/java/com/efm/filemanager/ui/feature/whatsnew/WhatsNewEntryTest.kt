package com.efm.filemanager.ui.feature.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val sampleEntries =
    listOf(
        WhatsNewEntry(versionCode = 10, titleRes = 1, bodyRes = 2),
        WhatsNewEntry(versionCode = 20, titleRes = 3, bodyRes = 4),
        WhatsNewEntry(versionCode = 30, titleRes = 5, bodyRes = 6),
    )

class WhatsNewEntryTest {
    @Test
    fun `a fresh install (null last-seen) shows nothing`() {
        assertTrue(entriesToShowFor(lastSeen = null, currentVersionCode = 30, allEntries = sampleEntries).isEmpty())
    }

    @Test
    fun `an upgrade shows every entry newer than last-seen, up to and including the current version`() {
        val shown = entriesToShowFor(lastSeen = 10, currentVersionCode = 30, allEntries = sampleEntries)
        assertEquals(listOf(sampleEntries[1], sampleEntries[2]), shown)
    }

    @Test
    fun `an upgrade that skips several versions still shows every entry in between`() {
        val shown = entriesToShowFor(lastSeen = 5, currentVersionCode = 25, allEntries = sampleEntries)
        assertEquals(listOf(sampleEntries[0], sampleEntries[1]), shown)
    }

    @Test
    fun `already having seen the current version shows nothing`() {
        assertTrue(entriesToShowFor(lastSeen = 30, currentVersionCode = 30, allEntries = sampleEntries).isEmpty())
    }
}
