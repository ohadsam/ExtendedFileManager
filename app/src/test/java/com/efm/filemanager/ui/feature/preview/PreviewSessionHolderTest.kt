package com.efm.filemanager.ui.feature.preview

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewSessionHolderTest {
    @Test
    fun `non-previewable entries are dropped and the tapped file's new index is used`() {
        val folderUri = mockk<Uri>()
        val docUri = mockk<Uri>()
        val imageUri = mockk<Uri>()
        val tappedUri = mockk<Uri>()
        val folder = entry(folderUri, "Photos", isDirectory = true)
        val doc = entry(docUri, "notes.docx")
        val image1 = entry(imageUri, "a.jpg", mimeType = "image/jpeg")
        val tapped = entry(tappedUri, "b.jpg", mimeType = "image/jpeg")

        val session = buildPreviewSession(listOf(folder, doc, image1, tapped), tapped)

        assertEquals(listOf(image1, tapped), session?.entries)
        assertEquals(1, session?.startIndex)
    }

    @Test
    fun `a tapped entry with no preview yields no session`() {
        val docUri = mockk<Uri>()
        val doc = entry(docUri, "notes.docx")

        assertNull(buildPreviewSession(listOf(doc), doc))
    }

    private fun entry(
        uri: Uri,
        name: String,
        isDirectory: Boolean = false,
        mimeType: String? = null,
    ) = FileEntry(
        uri = uri,
        documentId = name,
        name = name,
        isDirectory = isDirectory,
        size = 0L,
        lastModified = 0L,
        mimeType = mimeType,
        sourceApp = null,
    )
}
