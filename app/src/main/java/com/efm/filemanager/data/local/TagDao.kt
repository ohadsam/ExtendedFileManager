package com.efm.filemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY pinned DESC, name")
    fun observeAll(): Flow<List<TagEntity>>

    @Query(
        "SELECT file_tag_cross_refs.fileUri, file_tag_cross_refs.tagId, tags.name, tags.color, tags.pinned " +
            "FROM file_tag_cross_refs INNER JOIN tags ON file_tag_cross_refs.tagId = tags.id",
    )
    fun observeFileTagRows(): Flow<List<FileTagRow>>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToFile(crossRef: FileTagCrossRefEntity)

    @Query("DELETE FROM file_tag_cross_refs WHERE fileUri = :fileUri AND tagId = :tagId")
    suspend fun removeTagFromFile(
        fileUri: String,
        tagId: Long,
    )

    @Query(
        "DELETE FROM file_tag_cross_refs WHERE tagId = :fromTagId AND fileUri IN " +
            "(SELECT fileUri FROM file_tag_cross_refs WHERE tagId = :intoTagId)",
    )
    suspend fun deleteConflictingCrossRefs(
        fromTagId: Long,
        intoTagId: Long,
    )

    @Query("UPDATE file_tag_cross_refs SET tagId = :intoTagId WHERE tagId = :fromTagId")
    suspend fun reassignCrossRefs(
        fromTagId: Long,
        intoTagId: Long,
    )
}
