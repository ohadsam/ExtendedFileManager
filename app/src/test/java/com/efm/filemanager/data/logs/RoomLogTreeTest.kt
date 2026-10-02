package com.efm.filemanager.data.logs

import android.util.Log
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomLogTreeTest {
    private val tree = RoomLogTree(mockk(relaxed = true))

    @Test
    fun `verbose and debug are not loggable`() {
        assertFalse(tree.isLoggable(tag = null, priority = Log.VERBOSE))
        assertFalse(tree.isLoggable(tag = null, priority = Log.DEBUG))
    }

    @Test
    fun `info, warn, and error are loggable`() {
        assertTrue(tree.isLoggable(tag = null, priority = Log.INFO))
        assertTrue(tree.isLoggable(tag = null, priority = Log.WARN))
        assertTrue(tree.isLoggable(tag = null, priority = Log.ERROR))
    }
}
