package com.efm.filemanager.data.statistics

import com.efm.filemanager.data.local.FileEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsRepositoryTest {
    @Test
    fun `folders are ranked by file count descending and capped at the requested count`() {
        val files =
            listOf(
                entry(parentUri = "folder-a", name = "1.txt"),
                entry(parentUri = "folder-a", name = "2.txt"),
                entry(parentUri = "folder-b", name = "3.txt"),
                entry(parentUri = "folder-c", name = "4.txt"),
                entry(parentUri = "folder-c", name = "5.txt"),
                entry(parentUri = "folder-c", name = "6.txt"),
            )

        val top = files.topFolderCounts(topCount = 2)

        assertEquals(listOf("folder-c" to 3, "folder-a" to 2), top)
    }

    @Test
    fun `an empty file list yields no folders`() {
        assertEquals(emptyList<Pair<String, Int>>(), emptyList<FileEntryEntity>().topFolderCounts(topCount = 5))
    }

    private fun entry(
        parentUri: String,
        name: String,
    ) = FileEntryEntity(
        uri = "$parentUri/$name",
        parentUri = parentUri,
        documentId = name,
        name = name,
        isDirectory = false,
        size = 0L,
        lastModified = 0L,
        mimeType = null,
        ownerPackageName = null,
        sourceConfidence = null,
    )
}
