package com.efm.filemanager.data.prefs

import com.efm.filemanager.domain.model.AppearanceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesRepositoryTest {
    @Test
    fun `null stored value maps to system appearance`() {
        assertEquals(AppearanceMode.SYSTEM, appearanceModeFromStoredValue(null))
    }

    @Test
    fun `unrecognized stored value maps to system appearance`() {
        assertEquals(AppearanceMode.SYSTEM, appearanceModeFromStoredValue("not-a-mode"))
    }

    @Test
    fun `valid stored value maps to its appearance mode`() {
        assertEquals(AppearanceMode.DARK, appearanceModeFromStoredValue("DARK"))
    }
}
