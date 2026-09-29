package com.efm.filemanager.domain.model

import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewTypeTest {
    @Test
    fun `image mime type previews as image`() {
        assertEquals(PreviewType.IMAGE, entry("photo.jpg", mimeType = "image/jpeg").previewType())
    }

    @Test
    fun `video mime type previews as video`() {
        assertEquals(PreviewType.VIDEO, entry("clip.mp4", mimeType = "video/mp4").previewType())
    }

    @Test
    fun `audio mime type previews as audio`() {
        assertEquals(PreviewType.AUDIO, entry("song.mp3", mimeType = "audio/mpeg").previewType())
    }

    @Test
    fun `pdf mime type previews as pdf`() {
        assertEquals(PreviewType.PDF, entry("doc.pdf", mimeType = "application/pdf").previewType())
    }

    @Test
    fun `pdf extension previews as pdf even without a mime type`() {
        assertEquals(PreviewType.PDF, entry("doc.pdf", mimeType = null).previewType())
    }

    @Test
    fun `a non-pdf document has no preview`() {
        assertEquals(PreviewType.NONE, entry("report.docx", mimeType = null).previewType())
    }

    @Test
    fun `an archive has no preview`() {
        assertEquals(PreviewType.NONE, entry("backup.zip", mimeType = "application/zip").previewType())
    }

    @Test
    fun `a directory has no preview`() {
        assertEquals(PreviewType.NONE, entry("Photos", mimeType = null, isDirectory = true).previewType())
    }

    private fun entry(
        name: String,
        mimeType: String?,
        isDirectory: Boolean = false,
    ) = FileEntry(
        uri = mockk<Uri>(),
        documentId = name,
        name = name,
        isDirectory = isDirectory,
        size = 0L,
        lastModified = 0L,
        mimeType = mimeType,
        sourceApp = null,
    )
}
