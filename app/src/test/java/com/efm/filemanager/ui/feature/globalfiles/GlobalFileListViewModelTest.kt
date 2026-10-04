package com.efm.filemanager.ui.feature.globalfiles

import com.efm.filemanager.domain.model.FileCategory
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
}
