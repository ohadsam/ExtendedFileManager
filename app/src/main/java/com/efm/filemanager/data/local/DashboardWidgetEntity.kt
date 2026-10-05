package com.efm.filemanager.data.local

import androidx.room.Entity

/** Phase 19's per-widget visibility toggle -- [type] is a [com.efm.filemanager.domain.model.DashboardWidgetType] name. */
@Entity(tableName = "dashboard_widgets", primaryKeys = ["type"])
data class DashboardWidgetEntity(
    val type: String,
    val isEnabled: Boolean,
)
