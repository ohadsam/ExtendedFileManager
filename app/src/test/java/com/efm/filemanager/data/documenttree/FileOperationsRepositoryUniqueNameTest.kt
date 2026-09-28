package com.efm.filemanager.data.documenttree

import androidx.documentfile.provider.DocumentFile
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FileOperationsRepositoryUniqueNameTest {
    @Test
    fun `name with no collision is returned unchanged`() {
        val parent = mockk<DocumentFile> { every { findFile("file.txt") } returns null }

        assertEquals("file.txt", uniqueNameIn(parent, "file.txt"))
    }

    @Test
    fun `first collision appends (1) before the extension`() {
        val parent =
            mockk<DocumentFile> {
                every { findFile("file.txt") } returns mockk()
                every { findFile("file (1).txt") } returns null
            }

        assertEquals("file (1).txt", uniqueNameIn(parent, "file.txt"))
    }

    @Test
    fun `repeated collisions keep incrementing`() {
        val parent =
            mockk<DocumentFile> {
                every { findFile("file.txt") } returns mockk()
                every { findFile("file (1).txt") } returns mockk()
                every { findFile("file (2).txt") } returns null
            }

        assertEquals("file (2).txt", uniqueNameIn(parent, "file.txt"))
    }

    @Test
    fun `name without an extension is still numbered correctly`() {
        val parent =
            mockk<DocumentFile> {
                every { findFile("README") } returns mockk()
                every { findFile("README (1)") } returns null
            }

        assertEquals("README (1)", uniqueNameIn(parent, "README"))
    }
}
