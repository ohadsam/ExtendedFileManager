package com.efm.filemanager.data.metadata

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.efm.filemanager.R
import com.efm.filemanager.data.local.EfmDatabase
import com.efm.filemanager.data.local.FileTagCrossRefEntity
import com.efm.filemanager.data.local.TagCrossRefDao
import com.efm.filemanager.data.local.TagDao
import com.efm.filemanager.data.local.TagEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.TagColor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The tag catalog: a tag is defined once (name + color) and applied many-to-many, per
 * docs/PLAN.md Phase 9 -- not free-form-per-use like the rest of this app's other metadata.
 */
class TagRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val tagDao: TagDao,
        private val crossRefDao: TagCrossRefDao,
        private val database: EfmDatabase,
    ) {
        val tags: Flow<List<FileTag>> = tagDao.observeAll().map { entities -> entities.map { it.toDomain() } }

        val tagsByFileUri: Flow<Map<String, List<FileTag>>> =
            crossRefDao.observeFileTagRows().map { rows -> rows.groupBy({ it.fileUri }) { it.toDomain() } }

        /** No-ops once any tag exists -- called once at app startup, see [com.efm.filemanager.EfmApplication]. */
        suspend fun ensureDefaultTagsSeeded() {
            if (tagDao.count() > 0) return
            tagDao.insertAll(defaultTagEntities())
        }

        suspend fun createTag(
            name: String,
            color: TagColor,
        ): Long = tagDao.insert(TagEntity(name = name, color = color.name))

        suspend fun updateTag(
            tag: FileTag,
            name: String,
            color: TagColor,
        ) = tagDao.update(TagEntity(id = tag.id, name = name, color = color.name, pinned = tag.pinned))

        suspend fun setPinned(
            tag: FileTag,
            pinned: Boolean,
        ) = tagDao.update(TagEntity(id = tag.id, name = tag.name, color = tag.color.name, pinned = pinned))

        suspend fun deleteTag(tag: FileTag) = tagDao.delete(tag.id)

        /** Merges [from] into [into]: a file that already carried both keeps just [into]. */
        suspend fun mergeTags(
            from: FileTag,
            into: FileTag,
        ) {
            database.withTransaction {
                crossRefDao.deleteConflictingCrossRefs(from.id, into.id)
                crossRefDao.reassignCrossRefs(from.id, into.id)
                tagDao.delete(from.id)
            }
        }

        suspend fun addTagToFiles(
            fileUris: List<Uri>,
            tagId: Long,
        ) {
            fileUris.forEach { uri -> crossRefDao.addTagToFile(FileTagCrossRefEntity(fileUri = uri.toString(), tagId = tagId)) }
        }

        suspend fun removeTagFromFile(
            fileUri: Uri,
            tagId: Long,
        ) = crossRefDao.removeTagFromFile(fileUri.toString(), tagId)

        private fun defaultTagEntities(): List<TagEntity> =
            listOf(
                TagEntity(name = context.getString(R.string.tag_preset_important), color = TagColor.RED.name),
                TagEntity(name = context.getString(R.string.tag_preset_work), color = TagColor.BLUE.name),
                TagEntity(name = context.getString(R.string.tag_preset_to_sort), color = TagColor.YELLOW.name),
                TagEntity(name = context.getString(R.string.tag_preset_archive), color = TagColor.GRAY.name),
            )
    }
