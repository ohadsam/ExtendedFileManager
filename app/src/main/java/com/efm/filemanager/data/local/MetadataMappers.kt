package com.efm.filemanager.data.local

import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.TagColor

fun TagEntity.toDomain(): FileTag = FileTag(id = id, name = name, color = color.toTagColor(), pinned = pinned)

fun FileTagRow.toDomain(): FileTag = FileTag(id = tagId, name = name, color = color.toTagColor(), pinned = pinned)

fun FavoriteCollectionEntity.toDomain(): FavoriteCollection =
    FavoriteCollection(id = id, name = name, parentId = parentId, sortOrder = sortOrder)

private fun String.toTagColor(): TagColor = runCatching { TagColor.valueOf(this) }.getOrDefault(TagColor.GRAY)
