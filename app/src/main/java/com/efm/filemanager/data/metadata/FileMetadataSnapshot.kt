package com.efm.filemanager.data.metadata

import com.efm.filemanager.data.local.FavoriteEntity
import com.efm.filemanager.data.local.FileFlagsEntity
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag

/** A point-in-time join of every per-file metadata source, ready to enrich a plain [FileEntry]. */
data class FileMetadataSnapshot(
    val tagsByFileUri: Map<String, List<FileTag>>,
    val favoritesByFileUri: Map<String, FavoriteEntity>,
    val flagsByFileUri: Map<String, FileFlagsEntity>,
)

fun FileEntry.enrich(snapshot: FileMetadataSnapshot): FileEntry {
    val key = uri.toString()
    val favorite = snapshot.favoritesByFileUri[key]
    val flags = snapshot.flagsByFileUri[key]
    return copy(
        tags = snapshot.tagsByFileUri[key].orEmpty(),
        isFavorite = favorite != null,
        favoriteCollectionId = favorite?.collectionId,
        isLocked = flags?.locked ?: false,
        note = flags?.note,
        stagedAt = flags?.stagedAt,
    )
}
