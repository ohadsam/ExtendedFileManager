package com.efm.filemanager.data.local

import androidx.room.Entity

/**
 * Phase 19's per-widget visibility + position -- [type] is a
 * [com.efm.filemanager.domain.model.DashboardWidgetType] name, [sortOrder] this widget's place in
 * the user's own reordering (ascending), seeded from the catalog's declaration order.
 */
@Entity(tableName = "dashboard_widgets", primaryKeys = ["type"])
data class DashboardWidgetEntity(
    val type: String,
    val isEnabled: Boolean,
    val sortOrder: Int,
)
