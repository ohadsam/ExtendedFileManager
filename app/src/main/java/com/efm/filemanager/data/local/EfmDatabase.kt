package com.efm.filemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.efm.filemanager.data.audit.AuditEventDao
import com.efm.filemanager.data.audit.AuditEventEntity
import com.efm.filemanager.data.trash.TrashedFileDao
import com.efm.filemanager.data.trash.TrashedFileEntity

@Database(
    entities = [
        FileEntryEntity::class,
        AuditEventEntity::class,
        TrashedFileEntity::class,
        FileEntryFtsEntity::class,
        DuplicateFileEntity::class,
        TagEntity::class,
        FileTagCrossRefEntity::class,
        FavoriteCollectionEntity::class,
        FavoriteEntity::class,
        FileFlagsEntity::class,
        StorageRecommendationEntity::class,
        VaultEntryEntity::class,
        LogEntryEntity::class,
        CloudUploadEntity::class,
        StorageSnapshotEntity::class,
        StatsCacheEntity::class,
        StatsCacheLargestFileEntity::class,
        StatsCacheRecentFileEntity::class,
        StatsCacheFolderEntity::class,
        DashboardWidgetEntity::class,
        DashboardLayoutEntity::class,
        DashboardLayoutWidgetEntity::class,
    ],
    version = 19,
    exportSchema = false,
)
abstract class EfmDatabase : RoomDatabase() {
    abstract fun fileEntryDao(): FileEntryDao

    abstract fun fileSearchDao(): FileSearchDao

    abstract fun auditEventDao(): AuditEventDao

    abstract fun trashedFileDao(): TrashedFileDao

    abstract fun duplicateFileDao(): DuplicateFileDao

    abstract fun tagDao(): TagDao

    abstract fun tagCrossRefDao(): TagCrossRefDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun favoriteCollectionDao(): FavoriteCollectionDao

    abstract fun fileFlagsDao(): FileFlagsDao

    abstract fun storageRecommendationDao(): StorageRecommendationDao

    abstract fun vaultEntryDao(): VaultEntryDao

    abstract fun logEntryDao(): LogEntryDao

    abstract fun cloudUploadDao(): CloudUploadDao

    abstract fun storageSnapshotDao(): StorageSnapshotDao

    abstract fun statsCacheDao(): StatsCacheDao

    abstract fun statsCacheFolderDao(): StatsCacheFolderDao

    abstract fun dashboardWidgetDao(): DashboardWidgetDao

    abstract fun dashboardLayoutDao(): DashboardLayoutDao
}
