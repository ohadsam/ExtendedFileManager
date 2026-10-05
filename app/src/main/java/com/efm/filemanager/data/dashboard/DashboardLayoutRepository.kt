package com.efm.filemanager.data.dashboard

import com.efm.filemanager.data.local.DashboardWidgetDao
import com.efm.filemanager.data.local.DashboardWidgetEntity
import com.efm.filemanager.domain.model.DashboardWidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** One [DashboardWidgetType]'s current visibility -- the whole of what Phase 19's first customization slice persists. */
data class DashboardWidgetConfig(
    val type: DashboardWidgetType,
    val isEnabled: Boolean,
)

/**
 * Phase 19's widget-visibility persistence. [ensureSeeded] inserts every catalog entry as
 * enabled on first run (or whenever a later phase adds a new [DashboardWidgetType] entry) --
 * [DashboardWidgetDao.insertIfAbsent] is an `IGNORE`-conflict insert, so a widget that already
 * has a row never has its enabled/disabled state overwritten.
 */
class DashboardLayoutRepository
    @Inject
    constructor(
        private val dashboardWidgetDao: DashboardWidgetDao,
    ) {
        fun observeWidgets(): Flow<List<DashboardWidgetConfig>> =
            dashboardWidgetDao.observeAll().map { widgets -> widgets.mapNotNull { it.toConfig() } }

        suspend fun ensureSeeded() {
            val seeds = DashboardWidgetType.entries.map { DashboardWidgetEntity(type = it.name, isEnabled = true) }
            dashboardWidgetDao.insertIfAbsent(seeds)
        }

        suspend fun setEnabled(
            type: DashboardWidgetType,
            isEnabled: Boolean,
        ) = dashboardWidgetDao.setEnabled(type.name, isEnabled)
    }

internal fun DashboardWidgetEntity.toConfig(): DashboardWidgetConfig? =
    runCatching { DashboardWidgetType.valueOf(type) }.getOrNull()?.let { DashboardWidgetConfig(it, isEnabled) }
