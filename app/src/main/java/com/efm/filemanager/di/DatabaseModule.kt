package com.efm.filemanager.di

import android.content.Context
import androidx.room.Room
import com.efm.filemanager.data.audit.AuditEventDao
import com.efm.filemanager.data.local.DuplicateFileDao
import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.FileEntryDao
import com.efm.filemanager.data.local.FileSearchDao
import com.efm.filemanager.data.local.StorageRecommendationDao
import com.efm.filemanager.data.trash.TrashedFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): EfmDatabase =
        Room.databaseBuilder(context, EfmDatabase::class.java, "efm.db")
            // Pre-1.0, no real user data to preserve across a schema change yet --
            // proper migrations start once this app actually ships.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFileEntryDao(database: EfmDatabase): FileEntryDao = database.fileEntryDao()

    @Provides
    fun provideFileSearchDao(database: EfmDatabase): FileSearchDao = database.fileSearchDao()

    @Provides
    fun provideAuditEventDao(database: EfmDatabase): AuditEventDao = database.auditEventDao()

    @Provides
    fun provideTrashedFileDao(database: EfmDatabase): TrashedFileDao = database.trashedFileDao()

    @Provides
    fun provideDuplicateFileDao(database: EfmDatabase): DuplicateFileDao = database.duplicateFileDao()

    @Provides
    fun provideStorageRecommendationDao(database: EfmDatabase): StorageRecommendationDao = database.storageRecommendationDao()
}
