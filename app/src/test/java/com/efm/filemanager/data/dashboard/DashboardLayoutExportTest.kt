package com.efm.filemanager.data.dashboard

import com.efm.filemanager.domain.model.DashboardTemplateWidget
import com.efm.filemanager.domain.model.DashboardWidgetSize
import com.efm.filemanager.domain.model.DashboardWidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardLayoutExportTest {
    private val export =
        DashboardLayoutExport(
            name = "Cleanup day",
            widgets =
                listOf(
                    DashboardTemplateWidget(DashboardWidgetType.DUPLICATES, isEnabled = true, size = DashboardWidgetSize.DETAILED),
                    DashboardTemplateWidget(DashboardWidgetType.FAVORITES, isEnabled = false, size = DashboardWidgetSize.COMPACT),
                ),
        )

    @Test
    fun `a layout round-trips through serialize and parse unchanged`() {
        assertEquals(export, parseDashboardLayoutExport(export.serialize()))
    }

    @Test
    fun `blank text parses to nothing`() {
        assertNull(parseDashboardLayoutExport(""))
    }

    @Test
    fun `a name with no widget lines parses to nothing`() {
        assertNull(parseDashboardLayoutExport("Cleanup day"))
    }

    @Test
    fun `a malformed widget line is dropped rather than crashing the whole import`() {
        val text = "Cleanup day\nDUPLICATES|true|DETAILED\nnot-a-real-line\nFAVORITES|false|COMPACT"

        val parsed = parseDashboardLayoutExport(text)

        assertEquals(export, parsed)
    }

    @Test
    fun `an unrecognized widget type drops just that line`() {
        val text = "Cleanup day\nNOT_A_REAL_TYPE|true|DETAILED\nFAVORITES|false|COMPACT"

        val parsed = parseDashboardLayoutExport(text)

        assertEquals(listOf(DashboardWidgetType.FAVORITES), parsed?.widgets?.map { it.type })
    }
}
