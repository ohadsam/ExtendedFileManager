package com.efm.filemanager.data.dashboard

import com.efm.filemanager.data.local.DashboardWidgetDao
import com.efm.filemanager.data.local.DashboardWidgetEntity
import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.domain.model.DashboardWidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** One [DashboardWidgetType]'s current visibility -- the whole of what Phase 19's first customization slice persists. */
data class DashboardWidgetConfig(
    val type: DashboardWidgetType,
    val isEnabled: Boolean,
)

/**
 * Phase 19's widget-visibility and -order persistence. [ensureSeeded] inserts every catalog
 * entry as enabled, at its catalog declaration index, on first run (or whenever a later phase
 * adds a new [DashboardWidgetType] entry) -- [DashboardWidgetDao.insertIfAbsent] is an
 * `IGNORE`-conflict insert, so a widget that already has a row never has its enabled/disabled
 * state or position overwritten.
 */
class DashboardLayoutRepository
    @Inject
    constructor(
        private val dashboardWidgetDao: DashboardWidgetDao,
    ) {
        fun observeWidgets(): Flow<List<DashboardWidgetConfig>> =
            dashboardWidgetDao.observeAll().map { widgets -> widgets.mapNotNull { it.toConfig() } }

        suspend fun ensureSeeded() {
            val seeds =
                DashboardWidgetType.entries.mapIndexed { index, type ->
                    DashboardWidgetEntity(type = type.name, isEnabled = true, sortOrder = index)
                }
            dashboardWidgetDao.insertIfAbsent(seeds)
        }

        suspend fun setEnabled(
            type: DashboardWidgetType,
            isEnabled: Boolean,
        ) = dashboardWidgetDao.setEnabled(type.name, isEnabled)

        /**
         * Mirrors [com.efm.filemanager.data.metadata.FavoriteRepository.moveCollection]'s own
         * swap-sortOrder-with-a-neighbor approach -- a handful of widgets doesn't need anything
         * more elaborate than two single-row updates.
         */
        suspend fun moveWidget(
            type: DashboardWidgetType,
            direction: MoveDirection,
        ) {
            val widgets = dashboardWidgetDao.observeAll().first()
            val index = widgets.indexOfFirst { it.type == type.name }
            val swapIndex = if (direction == MoveDirection.UP) index - 1 else index + 1
            if (index < 0 || swapIndex !in widgets.indices) return
            val current = widgets[index]
            val swapWith = widgets[swapIndex]
            dashboardWidgetDao.updateWidget(current.copy(sortOrder = swapWith.sortOrder))
            dashboardWidgetDao.updateWidget(swapWith.copy(sortOrder = current.sortOrder))
        }
    }

internal fun DashboardWidgetEntity.toConfig(): DashboardWidgetConfig? =
    runCatching { DashboardWidgetType.valueOf(type) }.getOrNull()?.let { DashboardWidgetConfig(it, isEnabled) }
