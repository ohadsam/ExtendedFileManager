package com.efm.filemanager.data.sourceapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceAppHeuristicsTest {
    @Test
    fun `known WhatsApp folder names resolve to the WhatsApp package`() {
        assertEquals("com.whatsapp", SourceAppHeuristics.packageByFolderName["WhatsApp Images"])
        assertEquals("com.whatsapp", SourceAppHeuristics.packageByFolderName["WhatsApp Video"])
    }

    @Test
    fun `known Telegram folder names resolve to the Telegram package`() {
        assertEquals("org.telegram.messenger", SourceAppHeuristics.packageByFolderName["Telegram Images"])
    }

    @Test
    fun `unrecognized folder names resolve to nothing rather than a guess`() {
        assertNull(SourceAppHeuristics.packageByFolderName["Random Folder"])
    }
}
