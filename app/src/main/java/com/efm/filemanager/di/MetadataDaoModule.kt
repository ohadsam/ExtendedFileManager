package com.efm.filemanager.di

import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.FavoriteCollectionDao
import com.efm.filemanager.data.local.FavoriteDao
import com.efm.filemanager.data.local.FileFlagsDao
import com.efm.filemanager.data.local.TagCrossRefDao
import com.efm.filemanager.data.local.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO providers for Phase 9's per-file metadata (tags, favorites, lock/note) -- split out of [DatabaseModule] to keep it small. */
@Module
@InstallIn(SingletonComponent::class)
object MetadataDaoModule {
    @Provides
    fun provideTagDao(database: EfmDatabase): TagDao = database.tagDao()

    @Provides
    fun provideTagCrossRefDao(database: EfmDatabase): TagCrossRefDao = database.tagCrossRefDao()

    @Provides
    fun provideFavoriteDao(database: EfmDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideFavoriteCollectionDao(database: EfmDatabase): FavoriteCollectionDao = database.favoriteCollectionDao()

    @Provides
    fun provideFileFlagsDao(database: EfmDatabase): FileFlagsDao = database.fileFlagsDao()
}
