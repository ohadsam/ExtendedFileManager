package com.efm.filemanager.di

import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.StorageSnapshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO provider for Phase 18's storage-trend history -- split out of [DatabaseModule], same as [LogDaoModule]/[VaultDaoModule]. */
@Module
@InstallIn(SingletonComponent::class)
object StorageSnapshotDaoModule {
    @Provides
    fun provideStorageSnapshotDao(database: EfmDatabase): StorageSnapshotDao = database.storageSnapshotDao()
}
