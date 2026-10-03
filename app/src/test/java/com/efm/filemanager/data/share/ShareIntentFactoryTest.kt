package com.efm.filemanager.data.share

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareIntentFactoryTest {
    @Test
    fun `a single entry's mime type is used as-is`() {
        assertEquals("image/jpeg", commonMimeType(listOf(entry(mimeType = "image/jpeg"))))
    }

    @Test
    fun `entries sharing the same mime type keep it`() {
        val entries = listOf(entry(mimeType = "image/jpeg"), entry(mimeType = "image/jpeg"))
        assertEquals("image/jpeg", commonMimeType(entries))
    }

    @Test
    fun `entries with different mime types fall back to the wildcard`() {
        val entries = listOf(entry(mimeType = "image/jpeg"), entry(mimeType = "video/mp4"))
        assertEquals("*/*", commonMimeType(entries))
    }

    @Test
    fun `a null mime type is treated as the wildcard, not its own distinct type`() {
        val entries = listOf(entry(mimeType = "image/jpeg"), entry(mimeType = null))
        assertEquals("*/*", commonMimeType(entries))
    }

    private fun entry(mimeType: String?): FileEntry =
        FileEntry(
            uri = mockk<Uri>(),
            documentId = "doc",
            name = "file",
            isDirectory = false,
            size = 0L,
            lastModified = 0L,
            mimeType = mimeType,
            sourceApp = null,
        )
}
