package com.efm.filemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FileEntryEntity::class], version = 1, exportSchema = false)
abstract class EfmDatabase : RoomDatabase() {
    abstract fun fileEntryDao(): FileEntryDao
}
