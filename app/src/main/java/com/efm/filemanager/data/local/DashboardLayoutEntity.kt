package com.efm.filemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Phase 19's named saved layouts -- a user-created snapshot of [DashboardWidgetEntity]'s rows at the moment it was saved. */
@Entity(tableName = "dashboard_layouts")
data class DashboardLayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)
