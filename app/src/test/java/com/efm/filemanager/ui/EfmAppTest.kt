package com.efm.filemanager.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EfmAppTest {
    @Test
    fun `a phone-width screen is not expanded`() {
        assertFalse(isExpandedWidth(360))
    }

    @Test
    fun `just under the breakpoint is not expanded`() {
        assertFalse(isExpandedWidth(EXPANDED_WIDTH_BREAKPOINT_DP - 1))
    }

    @Test
    fun `exactly the breakpoint is expanded`() {
        assertTrue(isExpandedWidth(EXPANDED_WIDTH_BREAKPOINT_DP))
    }

    @Test
    fun `a tablet-width screen is expanded`() {
        assertTrue(isExpandedWidth(1024))
    }
}
