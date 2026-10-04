package com.efm.filemanager.domain.model

import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageRecommendationTest {
    @Test
    fun `a two-file group yields one duplicate recommendation per file, each counting one other copy`() {
        val groups = listOf(DuplicateGroup(hash = "abc", fileSize = 10L, files = listOf(entry("a.jpg"), entry("b.jpg"))))

        val recommendations = groups.toDuplicateRecommendations()

        assertEquals(2, recommendations.size)
        assertTrue(recommendations.all { it.category == StorageRecommendationCategory.DUPLICATE })
        assertTrue(recommendations.all { it.reason == RecommendationReason.DUPLICATE_CONTENT })
        assertTrue(recommendations.all { it.detail == "1" })
    }

    @Test
    fun `a three-file group counts two other copies per file`() {
        val files = listOf(entry("a.jpg"), entry("b.jpg"), entry("c.jpg"))
        val groups = listOf(DuplicateGroup(hash = "abc", fileSize = 10L, files = files))

        val recommendations = groups.toDuplicateRecommendations()

        assertEquals(3, recommendations.size)
        assertTrue(recommendations.all { it.detail == "2" })
    }

    @Test
    fun `multiple groups each flatten independently`() {
        val groups =
            listOf(
                DuplicateGroup(hash = "a", fileSize = 10L, files = listOf(entry("a1.jpg"), entry("a2.jpg"))),
                DuplicateGroup(hash = "b", fileSize = 20L, files = listOf(entry("b1.jpg"), entry("b2.jpg"), entry("b3.jpg"))),
            )

        val recommendations = groups.toDuplicateRecommendations()

        assertEquals(5, recommendations.size)
    }

    @Test
    fun `no groups yields no recommendations`() {
        assertEquals(emptyList<StorageRecommendation>(), emptyList<DuplicateGroup>().toDuplicateRecommendations())
    }

    private fun entry(name: String) =
        FileEntry(
            uri = mockk<Uri>(),
            documentId = name,
            name = name,
            isDirectory = false,
            size = 0L,
            lastModified = 0L,
            mimeType = null,
            sourceApp = null,
        )
}
