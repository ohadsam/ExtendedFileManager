package com.efm.filemanager.ui.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageOptionTest {
    @Test
    fun `empty language tag maps to system`() {
        assertEquals(LanguageOption.SYSTEM, languageOptionFromLanguageTag(""))
    }

    @Test
    fun `hebrew language tag maps to hebrew`() {
        assertEquals(LanguageOption.HEBREW, languageOptionFromLanguageTag("he"))
    }

    @Test
    fun `english language tag maps to english`() {
        assertEquals(LanguageOption.ENGLISH, languageOptionFromLanguageTag("en-US"))
    }

    @Test
    fun `unrecognized language tag maps to system`() {
        assertEquals(LanguageOption.SYSTEM, languageOptionFromLanguageTag("fr"))
    }

    @Test
    fun `system option has no language tag`() {
        assertNull(LanguageOption.SYSTEM.toLanguageTag())
    }

    @Test
    fun `english option maps to the en tag`() {
        assertEquals("en", LanguageOption.ENGLISH.toLanguageTag())
    }

    @Test
    fun `hebrew option maps to the he tag`() {
        assertEquals("he", LanguageOption.HEBREW.toLanguageTag())
    }
}
