package com.efm.filemanager.data.dashboard

import com.efm.filemanager.data.local.DashboardWidgetEntity
import com.efm.filemanager.domain.model.DashboardWidgetSize
import com.efm.filemanager.domain.model.DashboardWidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardLayoutRepositoryTest {
    @Test
    fun `a valid type and size name maps to its config`() {
        val entity = DashboardWidgetEntity(type = "DUPLICATES", isEnabled = false, sortOrder = 0, size = "COMPACT")

        val config = entity.toConfig()

        assertEquals(DashboardWidgetType.DUPLICATES, config?.type)
        assertEquals(false, config?.isEnabled)
        assertEquals(DashboardWidgetSize.COMPACT, config?.size)
    }

    @Test
    fun `an unrecognized type name maps to no config rather than crashing`() {
        assertNull(DashboardWidgetEntity(type = "not-a-real-widget", isEnabled = true, sortOrder = 0, size = "DETAILED").toConfig())
    }

    @Test
    fun `an unrecognized size name falls back to DETAILED rather than crashing`() {
        val entity = DashboardWidgetEntity(type = "INSIGHTS", isEnabled = true, sortOrder = 0, size = "not-a-real-size")

        assertEquals(DashboardWidgetSize.DETAILED, entity.toConfig()?.size)
    }
}
