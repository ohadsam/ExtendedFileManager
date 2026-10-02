package com.efm.filemanager.ui.feature.logs

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Test

class LogFormattersTest {
    @Test
    fun `known priorities get their own label`() {
        assertEquals("INFO", logPriorityLabel(Log.INFO))
        assertEquals("WARN", logPriorityLabel(Log.WARN))
        assertEquals("ERROR", logPriorityLabel(Log.ERROR))
        assertEquals("ASSERT", logPriorityLabel(Log.ASSERT))
    }

    @Test
    fun `an unrecognized priority falls back to a generic label`() {
        assertEquals("LOG", logPriorityLabel(Log.VERBOSE))
    }
}
