package com.efm.filemanager.data.sourceapp

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.efm.filemanager.domain.model.SourceApp
import com.efm.filemanager.domain.model.SourceConfidence
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Resolves which app a file most likely came from. See docs/PLAN.md Phase 1 for the
 * full rationale: MediaStore's OWNER_PACKAGE_NAME (API 29+) is the only exact signal
 * available to a third-party app; everything else is a folder-name heuristic, and
 * scoped storage means neither can ever be perfect. Never claims more certainty than
 * it has -- callers surface [SourceConfidence] to the user rather than hiding it.
 */
class SourceAppResolver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
    fun resolve(
        fileName: String,
        size: Long,
        parentFolderName: String?,
    ): SourceApp? = resolveExact(fileName, size) ?: resolveHeuristic(parentFolderName)

    private fun resolveExact(
        fileName: String,
        size: Long,
    ): SourceApp? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val projection = arrayOf(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
        val selectionArgs = arrayOf(fileName, size.toString())
        return runCatching {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
                if (columnIndex < 0 || !cursor.moveToFirst()) {
                    null
                } else {
                    cursor.getString(columnIndex)?.let { packageName ->
                        SourceApp(packageName, SourceConfidence.EXACT)
                    }
                }
            }
        }.getOrNull()
    }

    private fun resolveHeuristic(parentFolderName: String?): SourceApp? {
        val folderName = parentFolderName ?: return null
        val packageName = SourceAppHeuristics.packageByFolderName[folderName] ?: return null
        return SourceApp(packageName, SourceConfidence.HEURISTIC)
    }
}
