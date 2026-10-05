package com.efm.filemanager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardWidgetSizeTest {
    @Test
    fun `detailed toggles to compact`() {
        assertEquals(DashboardWidgetSize.COMPACT, DashboardWidgetSize.DETAILED.toggled())
    }

    @Test
    fun `compact toggles to detailed`() {
        assertEquals(DashboardWidgetSize.DETAILED, DashboardWidgetSize.COMPACT.toggled())
    }
}
