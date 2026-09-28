package com.efm.filemanager.data.local

import com.efm.filemanager.domain.model.SourceConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileEntryMapperTest {
    // toDomain() itself isn't exercised here: it calls android.net.Uri.parse, which is an
    // unmocked Android platform stub in a plain JVM unit test (this project has no Robolectric
    // dependency) and throws rather than returning a value. toSourceApp() is internal precisely
    // so this logic is testable without dragging in an Android-framework dependency for it.

    @Test
    fun `entity with exact source app maps to a domain SourceApp`() {
        val entity = testEntity(ownerPackageName = "com.whatsapp", sourceConfidence = "EXACT")

        val sourceApp = entity.toSourceApp()

        assertEquals("com.whatsapp", sourceApp?.packageName)
        assertEquals(SourceConfidence.EXACT, sourceApp?.confidence)
    }

    @Test
    fun `entity with no owner package maps to a null source app`() {
        val entity = testEntity(ownerPackageName = null, sourceConfidence = null)

        assertNull(entity.toSourceApp())
    }

    @Test
    fun `entity with an owner package but no confidence maps to a null source app`() {
        val entity = testEntity(ownerPackageName = "com.whatsapp", sourceConfidence = null)

        assertNull(entity.toSourceApp())
    }

    private fun testEntity(
        ownerPackageName: String?,
        sourceConfidence: String?,
    ) = FileEntryEntity(
        uri = "content://tree/primary%3A/document/primary%3ADownload%2Ffile.jpg",
        parentUri = "content://tree/primary%3A/document/primary%3ADownload",
        documentId = "primary:Download/file.jpg",
        name = "file.jpg",
        isDirectory = false,
        size = 1024L,
        lastModified = 0L,
        mimeType = "image/jpeg",
        ownerPackageName = ownerPackageName,
        sourceConfidence = sourceConfidence,
    )
}
