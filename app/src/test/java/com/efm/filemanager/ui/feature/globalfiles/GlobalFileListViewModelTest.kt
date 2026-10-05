package com.efm.filemanager.ui.feature.globalfiles

import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.GlobalFilesSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlobalFileListViewModelTest {
    @Test
    fun `the ALL sentinel maps to no category filter`() {
        assertNull(GLOBAL_FILES_ALL_CATEGORIES.toFileCategoryArg())
    }

    @Test
    fun `a valid category name maps to that category`() {
        assertEquals(FileCategory.IMAGE, "IMAGE".toFileCategoryArg())
    }

    @Test
    fun `an unrecognized value maps to no category filter rather than crashing`() {
        assertNull("not-a-real-category".toFileCategoryArg())
    }

    @Test
    fun `a null sort arg falls back to SIZE`() {
        assertEquals(GlobalFilesSort.SIZE, null.toGlobalFilesSortArg())
    }

    @Test
    fun `a valid sort name maps to that sort`() {
        assertEquals(GlobalFilesSort.RECENT, "RECENT".toGlobalFilesSortArg())
    }

    @Test
    fun `an unrecognized sort value falls back to SIZE rather than crashing`() {
        assertEquals(GlobalFilesSort.SIZE, "not-a-real-sort".toGlobalFilesSortArg())
    }
}
