package com.efm.filemanager.data.dashboard

import com.efm.filemanager.data.local.DashboardLayoutDao
import com.efm.filemanager.data.local.DashboardLayoutEntity
import com.efm.filemanager.data.local.DashboardLayoutWidgetEntity
import com.efm.filemanager.data.local.DashboardWidgetDao
import com.efm.filemanager.data.local.DashboardWidgetEntity
import com.efm.filemanager.data.metadata.MoveDirection
import com.efm.filemanager.domain.model.DashboardTemplate
import com.efm.filemanager.domain.model.DashboardWidgetSize
import com.efm.filemanager.domain.model.DashboardWidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** One [DashboardWidgetType]'s current visibility, position (via list order), and size. */
data class DashboardWidgetConfig(
    val type: DashboardWidgetType,
    val isEnabled: Boolean,
    val size: DashboardWidgetSize,
)

/** One of the user's own named [DashboardLayoutEntity] snapshots. */
data class DashboardSavedLayout(
    val id: Long,
    val name: String,
)

/**
 * Phase 19's widget-visibility/-order/-size persistence. [ensureSeeded] inserts every catalog
 * entry as enabled and [DashboardWidgetSize.DETAILED], at its catalog declaration index, on
 * first run (or whenever a later phase adds a new [DashboardWidgetType] entry) --
 * [DashboardWidgetDao.insertIfAbsent] is an `IGNORE`-conflict insert, so a widget that already
 * has a row never has its enabled/disabled state, position, or size overwritten.
 */
class DashboardLayoutRepository
    @Inject
    constructor(
        private val dashboardWidgetDao: DashboardWidgetDao,
        private val dashboardLayoutDao: DashboardLayoutDao,
    ) {
        fun observeWidgets(): Flow<List<DashboardWidgetConfig>> =
            dashboardWidgetDao.observeAll().map { widgets -> widgets.mapNotNull { it.toConfig() } }

        suspend fun ensureSeeded() {
            val seeds =
                DashboardWidgetType.entries.mapIndexed { index, type ->
                    DashboardWidgetEntity(type = type.name, isEnabled = true, sortOrder = index, size = DashboardWidgetSize.DETAILED.name)
                }
            dashboardWidgetDao.insertIfAbsent(seeds)
        }

        suspend fun setEnabled(
            type: DashboardWidgetType,
            isEnabled: Boolean,
        ) = dashboardWidgetDao.setEnabled(type.name, isEnabled)

        suspend fun setSize(
            type: DashboardWidgetType,
            size: DashboardWidgetSize,
        ) = dashboardWidgetDao.setSize(type.name, size.name)

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

        /**
         * Rewrites every widget's enabled/order/size to match [template] -- a full rewrite, not a
         * merge, since [DashboardTemplate] guarantees (see its own doc, checked by
         * `DashboardTemplateTest`) exactly one entry per [DashboardWidgetType].
         */
        suspend fun applyTemplate(template: DashboardTemplate) {
            template.widgets.forEachIndexed { index, templateWidget ->
                dashboardWidgetDao.updateWidget(
                    DashboardWidgetEntity(
                        type = templateWidget.type.name,
                        isEnabled = templateWidget.isEnabled,
                        sortOrder = index,
                        size = templateWidget.size.name,
                    ),
                )
            }
        }

        fun observeSavedLayouts(): Flow<List<DashboardSavedLayout>> =
            dashboardLayoutDao.observeLayouts().map { layouts -> layouts.map { DashboardSavedLayout(it.id, it.name) } }

        /** Snapshots today's widget rows as-is into a new named layout -- the exact inverse of [applySavedLayout]. */
        suspend fun saveCurrentAsLayout(name: String) {
            val current = dashboardWidgetDao.observeAll().first()
            val layoutId = dashboardLayoutDao.insertLayout(DashboardLayoutEntity(name = name, createdAt = System.currentTimeMillis()))
            val widgets =
                current.map { widget ->
                    DashboardLayoutWidgetEntity(layoutId, widget.type, widget.isEnabled, widget.sortOrder, widget.size)
                }
            dashboardLayoutDao.insertLayoutWidgets(widgets)
        }

        /** A saved layout's own rows always cover every catalog widget, since they were [saveCurrentAsLayout]'d from a full snapshot. */
        suspend fun applySavedLayout(layoutId: Long) {
            dashboardLayoutDao.getLayoutWidgets(layoutId).forEach { widget ->
                dashboardWidgetDao.updateWidget(
                    DashboardWidgetEntity(
                        type = widget.type,
                        isEnabled = widget.isEnabled,
                        sortOrder = widget.sortOrder,
                        size = widget.size,
                    ),
                )
            }
        }

        suspend fun renameSavedLayout(
            layoutId: Long,
            name: String,
        ) = dashboardLayoutDao.renameLayout(layoutId, name)

        /** No `@ForeignKey` on [DashboardLayoutWidgetEntity] (same convention as favorites) -- clean up its rows manually first. */
        suspend fun deleteSavedLayout(layoutId: Long) {
            dashboardLayoutDao.deleteLayoutWidgets(layoutId)
            dashboardLayoutDao.deleteLayout(layoutId)
        }
    }

/** An unrecognized or missing [DashboardWidgetEntity.size] value falls back to DETAILED -- never a silent crash over a cosmetic setting. */
internal fun DashboardWidgetEntity.toConfig(): DashboardWidgetConfig? =
    runCatching { DashboardWidgetType.valueOf(type) }.getOrNull()?.let { type ->
        val resolvedSize = runCatching { DashboardWidgetSize.valueOf(size) }.getOrDefault(DashboardWidgetSize.DETAILED)
        DashboardWidgetConfig(type, isEnabled, resolvedSize)
    }
