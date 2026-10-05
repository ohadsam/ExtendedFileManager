package com.efm.filemanager.domain.model

import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageStatsTest {
    @Test
    fun `empty list yields zeroed stats`() {
        val stats = emptyList<FileEntry>().toStorageStats()

        assertEquals(0L, stats.totalSize)
        assertEquals(0, stats.totalFileCount)
        assertEquals(emptyMap<FileCategory, Long>(), stats.sizeByCategory)
        assertEquals(emptyList<FileEntry>(), stats.largestFiles)
        assertEquals(emptyList<FileEntry>(), stats.recentlyModifiedFiles)
    }

    @Test
    fun `total size and count sum across every file`() {
        val files = listOf(entry("a.jpg", 100L, "image/jpeg"), entry("b.mp4", 200L, "video/mp4"))

        val stats = files.toStorageStats()

        assertEquals(300L, stats.totalSize)
        assertEquals(2, stats.totalFileCount)
    }

    @Test
    fun `size is bucketed by the same FileCategory Browse's filters use`() {
        val files =
            listOf(
                entry("a.jpg", 100L, "image/jpeg"),
                entry("b.jpg", 50L, "image/jpeg"),
                entry("c.mp4", 200L, "video/mp4"),
            )

        val stats = files.toStorageStats()

        assertEquals(150L, stats.sizeByCategory[FileCategory.IMAGE])
        assertEquals(200L, stats.sizeByCategory[FileCategory.VIDEO])
    }

    @Test
    fun `largest files are sorted descending and capped at the requested count`() {
        val files = listOf(entry("small.txt", 10L), entry("big.txt", 300L), entry("medium.txt", 100L))

        val stats = files.toStorageStats(largestCount = 2)

        assertEquals(listOf("big.txt", "medium.txt"), stats.largestFiles.map { it.name })
    }

    @Test
    fun `recently modified files are sorted newest-first and capped at the requested count`() {
        val files =
            listOf(entry("old.txt", lastModified = 100L), entry("new.txt", lastModified = 300L), entry("mid.txt", lastModified = 200L))

        val stats = files.toStorageStats(largestCount = 2)

        assertEquals(listOf("new.txt", "mid.txt"), stats.recentlyModifiedFiles.map { it.name })
    }

    private fun entry(
        name: String,
        size: Long = 0L,
        mimeType: String? = null,
        lastModified: Long = 0L,
    ) = FileEntry(
        uri = mockk<Uri>(),
        documentId = name,
        name = name,
        isDirectory = false,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        sourceApp = null,
    )
}
