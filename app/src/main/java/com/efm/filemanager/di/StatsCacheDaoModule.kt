package com.efm.filemanager.di

import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.StatsCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO provider for Phase 18's incremental-recompute cache -- split out of [DatabaseModule], same as [StorageSnapshotDaoModule]. */
@Module
@InstallIn(SingletonComponent::class)
object StatsCacheDaoModule {
    @Provides
    fun provideStatsCacheDao(database: EfmDatabase): StatsCacheDao = database.statsCacheDao()
}
