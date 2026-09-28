package com.efm.filemanager.ui.feature.browse

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveDialogsTest {
    @Test
    fun `name without a zip extension gets one appended`() {
        assertEquals("Archive.zip", ensureZipExtension("Archive"))
    }

    @Test
    fun `name with a lowercase zip extension is left unchanged`() {
        assertEquals("Archive.zip", ensureZipExtension("Archive.zip"))
    }

    @Test
    fun `name with an uppercase zip extension is left unchanged`() {
        assertEquals("Archive.ZIP", ensureZipExtension("Archive.ZIP"))
    }
}
