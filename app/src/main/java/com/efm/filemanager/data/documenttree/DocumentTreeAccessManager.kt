package com.efm.filemanager.data.documenttree

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Wraps Storage Access Framework tree-permission grants. Deliberately has no storage
 * of its own -- [android.content.ContentResolver.persistedUriPermissions] is already
 * the durable, OS-managed source of truth for "which folders did the user grant us,"
 * so re-reading it beats maintaining a parallel copy that could drift.
 */
class DocumentTreeAccessManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun grantedTreeUris(): List<Uri> =
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri }

        fun persistAccess(treeUri: Uri) {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }

        fun rootLabel(treeUri: Uri): String {
            val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            val label = documentId?.substringAfterLast(':')
            return label?.ifBlank { null } ?: "Storage"
        }
    }
