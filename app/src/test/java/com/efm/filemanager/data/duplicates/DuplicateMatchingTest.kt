package com.efm.filemanager.data.duplicates

import com.efm.filemanager.data.local.FileEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateMatchingTest {
    @Test
    fun `files with a unique size are dropped, sizes shared by 2+ files are kept`() {
        val files =
            listOf(
                entry("a.txt", size = 100),
                entry("b.txt", size = 100),
                entry("c.txt", size = 200),
            )

        val groups = candidateSizeGroups(files)

        assertEquals(1, groups.size)
        assertEquals(setOf("a.txt", "b.txt"), groups.single().map { it.name }.toSet())
    }

    @Test
    fun `candidates sharing a hash are grouped, singletons are dropped`() {
        val a = entry("a.txt")
        val b = entry("b.txt")
        val c = entry("c.txt")
        val hashed = listOf(a to "hash1", b to "hash1", c to "hash2")

        val groups = groupDuplicateCandidates(hashed)

        assertEquals(1, groups.size)
        assertEquals("hash1", groups.single().first)
        assertEquals(setOf("a.txt", "b.txt"), groups.single().second.map { it.name }.toSet())
    }

    private fun entry(
        name: String,
        size: Long = 0L,
    ) = FileEntryEntity(
        uri = "content://tree/primary/document/$name",
        parentUri = "content://tree/primary/document/parent",
        documentId = name,
        name = name,
        isDirectory = false,
        size = size,
        lastModified = 0L,
        mimeType = null,
        ownerPackageName = null,
        sourceConfidence = null,
    )
}
