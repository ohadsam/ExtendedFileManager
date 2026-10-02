package com.efm.filemanager.di

import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.LogEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO provider for Phase 12's log store -- split out of [DatabaseModule], same as [VaultDaoModule]. */
@Module
@InstallIn(SingletonComponent::class)
object LogDaoModule {
    @Provides
    fun provideLogEntryDao(database: EfmDatabase): LogEntryDao = database.logEntryDao()
}
