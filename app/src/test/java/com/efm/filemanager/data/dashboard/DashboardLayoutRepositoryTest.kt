package com.efm.filemanager.data.dashboard

import com.efm.filemanager.data.local.DashboardWidgetEntity
import com.efm.filemanager.domain.model.DashboardWidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardLayoutRepositoryTest {
    @Test
    fun `a valid type name maps to its config`() {
        val entity = DashboardWidgetEntity(type = "DUPLICATES", isEnabled = false)

        val config = entity.toConfig()

        assertEquals(DashboardWidgetType.DUPLICATES, config?.type)
        assertEquals(false, config?.isEnabled)
    }

    @Test
    fun `an unrecognized type name maps to no config rather than crashing`() {
        assertNull(DashboardWidgetEntity(type = "not-a-real-widget", isEnabled = true).toConfig())
    }
}
