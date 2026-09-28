package com.efm.filemanager.data.trash

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrashedFileDao {
    @Insert
    suspend fun insert(entity: TrashedFileEntity): Long

    @Query("SELECT * FROM trashed_files WHERE id = :id")
    suspend fun getById(id: Long): TrashedFileEntity?

    @Delete
    suspend fun delete(entity: TrashedFileEntity)
}
