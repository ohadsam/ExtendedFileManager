package com.efm.filemanager.di

import com.efm.filemanager.data.local.CloudUploadDao
import com.efm.filemanager.data.local.EfmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO provider for Phase 16's cloud-upload tracking -- split out of [DatabaseModule], same as [LogDaoModule]/[VaultDaoModule]. */
@Module
@InstallIn(SingletonComponent::class)
object CloudUploadDaoModule {
    @Provides
    fun provideCloudUploadDao(database: EfmDatabase): CloudUploadDao = database.cloudUploadDao()
}
