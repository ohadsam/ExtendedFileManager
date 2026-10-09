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

    @Query("SELECT * FROM dashboard_layouts WHERE id = :id")
    suspend fun getLayout(id: Long): DashboardLayoutEntity?

    @Query("UPDATE dashboard_layouts SET name = :name WHERE id = :id")
    suspend fun renameLayout(
        id: Long,
        name: String,
    )

    @Query("DELETE FROM dashboard_layouts WHERE id = :id")
    suspend fun deleteLayout(id: Long)

    @Query("DELETE FROM dashboard_layout_widgets WHERE layoutId = :layoutId")
    suspend fun deleteLayoutWidgets(layoutId: Long)
}
