package com.efm.filemanager.data.local

import androidx.room.Entity

/**
 * One widget's saved state within a [DashboardLayoutEntity] -- same shape as
 * [DashboardWidgetEntity], keyed by which saved layout it belongs to.
 */
@Entity(tableName = "dashboard_layout_widgets", primaryKeys = ["layoutId", "type"])
data class DashboardLayoutWidgetEntity(
    val layoutId: Long,
    val type: String,
    val isEnabled: Boolean,
    val sortOrder: Int,
    val size: String,
)
