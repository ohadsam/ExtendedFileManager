package com.efm.filemanager.di

import android.content.Context
import androidx.room.Room
import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.FileEntryDao
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
    ): EfmDatabase = Room.databaseBuilder(context, EfmDatabase::class.java, "efm.db").build()

    @Provides
    fun provideFileEntryDao(database: EfmDatabase): FileEntryDao = database.fileEntryDao()
}
