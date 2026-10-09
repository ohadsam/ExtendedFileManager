package com.efm.filemanager.data.statistics

import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.StorageSnapshotEntity
import com.efm.filemanager.domain.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsRepositoryTest {
    @Test
    fun `rows are grouped by category, each keeping day-ascending order`() {
        val rows =
            listOf(
                StorageSnapshotEntity(day = 1, category = "IMAGE", bytes = 100),
                StorageSnapshotEntity(day = 1, category = "VIDEO", bytes = 500),
                StorageSnapshotEntity(day = 2, category = "IMAGE", bytes = 150),
                StorageSnapshotEntity(day = 2, category = "VIDEO", bytes = 600),
            )

        val trends = rows.toCategoryTrends()

        assertEquals(listOf(100L, 150L), trends[FileCategory.IMAGE])
        assertEquals(listOf(500L, 600L), trends[FileCategory.VIDEO])
    }

    @Test
    fun `a row from an unrecognized category name is dropped, not crashed on`() {
        val rows = listOf(StorageSnapshotEntity(day = 1, category = "NOT_A_REAL_CATEGORY", bytes = 42))

        assertEquals(emptyMap<FileCategory, List<Long>>(), rows.toCategoryTrends())
    }

    @Test
    fun `an empty row list yields no categories`() {
        assertEquals(emptyMap<FileCategory, List<Long>>(), emptyList<StorageSnapshotEntity>().toCategoryTrends())
    }

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
