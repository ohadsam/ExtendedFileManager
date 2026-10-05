package com.efm.filemanager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardTemplateTest {
    @Test
    fun `every template lists every catalog widget type exactly once`() {
        DashboardTemplate.entries.forEach { template ->
            assertEquals(
                "${template.name} must list every DashboardWidgetType exactly once",
                DashboardWidgetType.entries.toSet(),
                template.widgets.map { it.type }.toSet(),
            )
            assertEquals(
                "${template.name} must not repeat a DashboardWidgetType",
                DashboardWidgetType.entries.size,
                template.widgets.size,
            )
        }
    }
}
