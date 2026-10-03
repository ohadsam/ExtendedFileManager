package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudUploadDao {
    @Insert
    suspend fun insert(entity: CloudUploadEntity): Long

    @Update
    suspend fun update(entity: CloudUploadEntity)

    @Delete
    suspend fun delete(entity: CloudUploadEntity)

    @Query("SELECT * FROM cloud_uploads WHERE id = :id")
    suspend fun getById(id: Long): CloudUploadEntity?

    @Query("SELECT * FROM cloud_uploads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CloudUploadEntity>>
}
