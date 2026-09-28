package com.efm.filemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.efm.filemanager.data.audit.AuditEventDao
import com.efm.filemanager.data.audit.AuditEventEntity
import com.efm.filemanager.data.trash.TrashedFileDao
import com.efm.filemanager.data.trash.TrashedFileEntity

@Database(
    entities = [FileEntryEntity::class, AuditEventEntity::class, TrashedFileEntity::class, FileEntryFtsEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class EfmDatabase : RoomDatabase() {
    abstract fun fileEntryDao(): FileEntryDao

    abstract fun fileSearchDao(): FileSearchDao

    abstract fun auditEventDao(): AuditEventDao

    abstract fun trashedFileDao(): TrashedFileDao
}
