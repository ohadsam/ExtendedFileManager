package com.efm.filemanager.data.prefs

import com.efm.filemanager.domain.model.AppearanceMode
import com.efm.filemanager.domain.model.ViewMode
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

    @Test
    fun `null stored value maps to list view mode`() {
        assertEquals(ViewMode.LIST, viewModeFromStoredValue(null))
    }

    @Test
    fun `unrecognized stored value maps to list view mode`() {
        assertEquals(ViewMode.LIST, viewModeFromStoredValue("not-a-mode"))
    }

    @Test
    fun `valid stored value maps to its view mode`() {
        assertEquals(ViewMode.GRID, viewModeFromStoredValue("GRID"))
    }
}
