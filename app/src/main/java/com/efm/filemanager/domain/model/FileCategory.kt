package com.efm.filemanager.domain.model

enum class FileCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    FOLDER,
    OTHER,
}

private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz")
private val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "odt", "ods", "odp")
private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

fun FileEntry.category(): FileCategory {
    val extension = name.substringAfterLast('.', "").lowercase()
    return when {
        isDirectory -> FileCategory.FOLDER
        mimeType?.startsWith("image/") == true -> FileCategory.IMAGE
        mimeType?.startsWith("video/") == true -> FileCategory.VIDEO
        mimeType?.startsWith("audio/") == true -> FileCategory.AUDIO
        mimeType == APK_MIME_TYPE -> FileCategory.APK
        mimeType == "application/zip" || extension in ARCHIVE_EXTENSIONS -> FileCategory.ARCHIVE
        mimeType?.startsWith("text/") == true || mimeType == "application/pdf" || extension in DOCUMENT_EXTENSIONS ->
            FileCategory.DOCUMENT
        else -> FileCategory.OTHER
    }
}
