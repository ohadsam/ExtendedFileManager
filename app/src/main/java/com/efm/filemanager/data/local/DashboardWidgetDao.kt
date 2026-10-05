package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardWidgetDao {
    @Query("SELECT * FROM dashboard_widgets ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<DashboardWidgetEntity>>

    /** IGNORE, not REPLACE -- seeding never overwrites a widget's existing enabled/disabled state or position. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(widgets: List<DashboardWidgetEntity>)

    @Query("UPDATE dashboard_widgets SET isEnabled = :isEnabled WHERE type = :type")
    suspend fun setEnabled(
        type: String,
        isEnabled: Boolean,
    )

    @Query("UPDATE dashboard_widgets SET size = :size WHERE type = :type")
    suspend fun setSize(
        type: String,
        size: String,
    )

    /** Used only to swap two widgets' [DashboardWidgetEntity.sortOrder] when reordering. */
    @Update
    suspend fun updateWidget(widget: DashboardWidgetEntity)
}
