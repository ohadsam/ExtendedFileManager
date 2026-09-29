package com.efm.filemanager.domain.model

enum class PreviewType { IMAGE, VIDEO, AUDIO, PDF, NONE }

/** What kind of preview [FileEntry] supports, if any -- reuses [category] rather than re-deriving mime/extension checks. */
fun FileEntry.previewType(): PreviewType =
    when (category()) {
        FileCategory.IMAGE -> PreviewType.IMAGE
        FileCategory.VIDEO -> PreviewType.VIDEO
        FileCategory.AUDIO -> PreviewType.AUDIO
        FileCategory.DOCUMENT -> if (isPdf()) PreviewType.PDF else PreviewType.NONE
        FileCategory.ARCHIVE, FileCategory.APK, FileCategory.FOLDER, FileCategory.OTHER -> PreviewType.NONE
    }

private fun FileEntry.isPdf(): Boolean = mimeType == "application/pdf" || name.substringAfterLast('.', "").equals("pdf", ignoreCase = true)
