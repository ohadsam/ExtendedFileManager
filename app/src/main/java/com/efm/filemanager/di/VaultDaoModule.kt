package com.efm.filemanager.di

import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.VaultEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO provider for Phase 11's encrypted vault -- split out of [DatabaseModule], same as [MetadataDaoModule]. */
@Module
@InstallIn(SingletonComponent::class)
object VaultDaoModule {
    @Provides
    fun provideVaultEntryDao(database: EfmDatabase): VaultEntryDao = database.vaultEntryDao()
}
