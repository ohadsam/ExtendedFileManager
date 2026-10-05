package com.efm.filemanager.di

import com.efm.filemanager.data.local.DashboardWidgetDao
import com.efm.filemanager.data.local.EfmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO provider for Phase 19's widget-visibility persistence -- split out of [DatabaseModule], same as [StatsCacheDaoModule]. */
@Module
@InstallIn(SingletonComponent::class)
object DashboardWidgetDaoModule {
    @Provides
    fun provideDashboardWidgetDao(database: EfmDatabase): DashboardWidgetDao = database.dashboardWidgetDao()
}
