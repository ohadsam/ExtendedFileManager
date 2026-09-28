package com.efm.filemanager.ui.feature.browse

import org.junit.Assert.assertEquals
import org.junit.Test

class FileFormattersTest {

    @Test
    fun `bytes under 1024 are shown as bytes`() {
        assertEquals("512 B", formatFileSize(512))
    }

    @Test
    fun `kilobyte range is formatted with one decimal`() {
        assertEquals("1.5 KB", formatFileSize(1536))
    }

    @Test
    fun `megabyte range is formatted with one decimal`() {
        assertEquals("2.0 MB", formatFileSize(2 * 1024 * 1024L))
    }

    @Test
    fun `gigabyte range is formatted with one decimal`() {
        assertEquals("1.0 GB", formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun `size never grows past the largest known unit`() {
        val huge = 5L * 1024 * 1024 * 1024 * 1024 * 1024
        assertEquals(true, formatFileSize(huge).endsWith("TB"))
    }
}
