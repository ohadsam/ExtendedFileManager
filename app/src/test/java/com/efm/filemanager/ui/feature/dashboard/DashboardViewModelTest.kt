package com.efm.filemanager.ui.feature.dashboard

import com.efm.filemanager.data.dashboard.DashboardWidgetConfig
import com.efm.filemanager.domain.model.DashboardWidgetType
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardViewModelTest {
    @Test
    fun `configs come back in catalog declaration order regardless of input order`() {
        val configs =
            listOf(
                DashboardWidgetConfig(DashboardWidgetType.FAVORITES, isEnabled = true),
                DashboardWidgetConfig(DashboardWidgetType.STORAGE_SUMMARY, isEnabled = false),
                DashboardWidgetConfig(DashboardWidgetType.DUPLICATES, isEnabled = true),
            )

        val ordered = configs.inCatalogOrder()

        assertEquals(
            listOf(DashboardWidgetType.STORAGE_SUMMARY, DashboardWidgetType.DUPLICATES, DashboardWidgetType.FAVORITES),
            ordered.map { it.type },
        )
    }

    @Test
    fun `a catalog entry with no matching config is simply absent, not a crash`() {
        val configs = listOf(DashboardWidgetConfig(DashboardWidgetType.INSIGHTS, isEnabled = true))

        val ordered = configs.inCatalogOrder()

        assertEquals(listOf(DashboardWidgetType.INSIGHTS), ordered.map { it.type })
    }
}
