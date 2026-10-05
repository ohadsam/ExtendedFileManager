package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardLayoutDao {
    @Query("SELECT * FROM dashboard_layouts ORDER BY createdAt ASC")
    fun observeLayouts(): Flow<List<DashboardLayoutEntity>>

    @Insert
    suspend fun insertLayout(layout: DashboardLayoutEntity): Long

    @Insert
    suspend fun insertLayoutWidgets(widgets: List<DashboardLayoutWidgetEntity>)

    @Query("SELECT * FROM dashboard_layout_widgets WHERE layoutId = :layoutId ORDER BY sortOrder ASC")
    suspend fun getLayoutWidgets(layoutId: Long): List<DashboardLayoutWidgetEntity>
}
