package com.efm.filemanager.data.documenttree

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.efm.filemanager.data.audit.AuditAction
import com.efm.filemanager.data.audit.AuditLogger
import com.efm.filemanager.data.metadata.FileFlagsRepository
import com.efm.filemanager.data.security.PathGuard
import com.efm.filemanager.data.trash.TrashedFileDao
import com.efm.filemanager.data.trash.TrashedFileEntity
import com.efm.filemanager.domain.model.FileEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

private const val FILE_MIME_TYPE = "application/octet-stream"

/**
 * Every mutating file operation Phase 3 introduces (create/rename/delete/move/copy),
 * each wrapped in a [Result] so the UI can show a real error instead of crashing, and
 * each logging an audit entry (Phase 13's groundwork) whether it succeeds or fails.
 * Delete is soft: the item moves into a per-folder `.efm_trash` subfolder so a
 * just-deleted item can be undone, rather than being removed outright. [delete] refuses
 * (not just warns about) a locked item -- see docs/PLAN.md Phase 9 -- which also covers
 * Phase 4's Extract & Replace and Phase 6's duplicate-delete, since both route through
 * this same [delete]. A failure also goes to `Timber` (Phase 12's diagnostic log), since
 * the audit entry alone doesn't carry a stack trace.
 */
class FileOperationsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val documentTreeRepository: DocumentTreeRepository,
        private val fileFlagsRepository: FileFlagsRepository,
        private val trashedFileDao: TrashedFileDao,
        private val auditLogger: AuditLogger,
    ) {
        suspend fun createFolder(
            parentUri: Uri,
            name: String,
        ): Result<Unit> =
            runOperation(AuditAction.CREATE_FOLDER, name, parentUri) {
                PathGuard.checkAllowed(parentUri)
                PathGuard.checkNameAllowed(name)
                val parent = requireTreeDocument(context, parentUri)
                requireNotNull(parent.createDirectory(name)) { "Could not create folder" }
                documentTreeRepository.refresh(parentUri)
            }

        suspend fun createFile(
            parentUri: Uri,
            name: String,
        ): Result<Unit> =
            runOperation(AuditAction.CREATE_FILE, name, parentUri) {
                PathGuard.checkAllowed(parentUri)
                PathGuard.checkNameAllowed(name)
                val parent = requireTreeDocument(context, parentUri)
                requireNotNull(parent.createFile(FILE_MIME_TYPE, name)) { "Could not create file" }
                documentTreeRepository.refresh(parentUri)
            }

        suspend fun rename(
            entry: FileEntry,
            parentUri: Uri,
            newName: String,
        ): Result<Unit> =
            runOperation(AuditAction.RENAME, entry.name, entry.uri, detail = newName) {
                PathGuard.checkAllowed(entry.uri)
                PathGuard.checkNameAllowed(newName)
                val document = requireSingleDocument(context, entry.uri)
                require(document.renameTo(newName)) { "Could not rename ${entry.name}" }
                documentTreeRepository.refresh(parentUri)
            }

        suspend fun delete(
            entry: FileEntry,
            parentUri: Uri,
        ): Result<Long> {
            if (fileFlagsRepository.isLocked(entry.uri)) {
                auditLogger.log(AuditAction.DELETE, entry.name, entry.uri, success = false, detail = "locked")
                return Result.failure(LockedFileException(entry.name))
            }
            return runCatching {
                withContext(Dispatchers.IO) {
                    PathGuard.checkAllowed(entry.uri)
                    val parent = requireTreeDocument(context, parentUri)
                    val trashFolder = parent.findFile(TRASH_FOLDER_NAME) ?: parent.createDirectory(TRASH_FOLDER_NAME)
                    requireNotNull(trashFolder) { "Could not prepare trash folder" }
                    val trashedUri = moveDocument(context, entry.uri, parentUri, trashFolder.uri)
                    val id =
                        trashedFileDao.insert(
                            TrashedFileEntity(
                                trashUri = trashedUri.toString(),
                                originalParentUri = parentUri.toString(),
                                originalName = entry.name,
                                trashedAt = System.currentTimeMillis(),
                            ),
                        )
                    documentTreeRepository.refresh(parentUri)
                    id
                }
            }.onSuccess { auditLogger.log(AuditAction.DELETE, entry.name, entry.uri, success = true) }
                .onFailure {
                    auditLogger.log(AuditAction.DELETE, entry.name, entry.uri, success = false, detail = it.message)
                }
        }

        suspend fun restore(trashedFileId: Long): Result<Unit> {
            val trashed =
                trashedFileDao.getById(trashedFileId)
                    ?: return Result.failure(IllegalStateException("Nothing to restore"))
            val originalParentUri = Uri.parse(trashed.originalParentUri)
            return runOperation(AuditAction.RESTORE, trashed.originalName, originalParentUri) {
                PathGuard.checkAllowed(originalParentUri)
                val trashUri = Uri.parse(trashed.trashUri)
                val trashFolderUri =
                    requireTreeDocument(context, originalParentUri).findFile(TRASH_FOLDER_NAME)?.uri
                        ?: error("Trash folder is gone")
                moveDocument(context, trashUri, trashFolderUri, originalParentUri)
                trashedFileDao.delete(trashed)
                documentTreeRepository.refresh(originalParentUri)
            }
        }

        suspend fun move(
            entry: FileEntry,
            sourceParentUri: Uri,
            targetParentUri: Uri,
        ): Result<Unit> =
            runOperation(AuditAction.MOVE, entry.name, entry.uri, detail = targetParentUri.toString()) {
                PathGuard.checkAllowed(entry.uri)
                PathGuard.checkAllowed(targetParentUri)
                moveDocument(context, entry.uri, sourceParentUri, targetParentUri)
                documentTreeRepository.refresh(sourceParentUri)
                documentTreeRepository.refresh(targetParentUri)
            }

        suspend fun copy(
            entry: FileEntry,
            targetParentUri: Uri,
        ): Result<Unit> =
            runOperation(AuditAction.COPY, entry.name, entry.uri, detail = targetParentUri.toString()) {
                PathGuard.checkAllowed(targetParentUri)
                val source = requireSingleDocument(context, entry.uri)
                val targetParent = requireTreeDocument(context, targetParentUri)
                requireNotNull(copyRecursively(context, source, targetParent)) { "Could not copy ${entry.name}" }
                documentTreeRepository.refresh(targetParentUri)
            }

        private suspend fun runOperation(
            action: AuditAction,
            targetName: String,
            targetUri: Uri,
            detail: String? = null,
            block: suspend () -> Unit,
        ): Result<Unit> =
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onSuccess { auditLogger.log(action, targetName, targetUri, success = true, detail = detail) }
                .onFailure {
                    auditLogger.log(action, targetName, targetUri, success = false, detail = it.message ?: detail)
                    Timber.e(it, "%s failed for %s", action.name, targetName)
                }
    }

internal fun requireTreeDocument(
    context: Context,
    uri: Uri,
): DocumentFile = requireNotNull(DocumentFile.fromTreeUri(context, uri)) { "Folder is no longer accessible" }

internal fun requireSingleDocument(
    context: Context,
    uri: Uri,
): DocumentFile = requireNotNull(DocumentFile.fromSingleUri(context, uri)) { "File is no longer accessible" }

private fun moveDocument(
    context: Context,
    sourceUri: Uri,
    sourceParentUri: Uri,
    targetParentUri: Uri,
): Uri =
    runCatching {
        DocumentsContract.moveDocument(context.contentResolver, sourceUri, sourceParentUri, targetParentUri)
    }.getOrNull() ?: fallbackMove(context, sourceUri, targetParentUri)

private fun fallbackMove(
    context: Context,
    sourceUri: Uri,
    targetParentUri: Uri,
): Uri {
    val source = requireSingleDocument(context, sourceUri)
    val targetParent = requireTreeDocument(context, targetParentUri)
    val copied = requireNotNull(copyRecursively(context, source, targetParent)) { "Could not move ${source.name}" }
    require(source.delete()) { "Could not remove original after copying ${source.name}" }
    return copied.uri
}

private fun copyRecursively(
    context: Context,
    source: DocumentFile,
    targetParent: DocumentFile,
): DocumentFile? {
    val sourceName = source.name ?: return null
    val name = uniqueNameIn(targetParent, sourceName)
    return if (source.isDirectory) {
        targetParent.createDirectory(name)?.also { newDir ->
            source.listFiles().forEach { child -> copyRecursively(context, child, newDir) }
        }
    } else {
        copyFile(context, source, targetParent, name)
    }
}

private fun copyFile(
    context: Context,
    source: DocumentFile,
    targetParent: DocumentFile,
    name: String,
): DocumentFile? {
    val newFile = targetParent.createFile(source.type ?: FILE_MIME_TYPE, name) ?: return null
    context.contentResolver.openInputStream(source.uri)?.use { input ->
        context.contentResolver.openOutputStream(newFile.uri)?.use { output -> input.copyTo(output) }
    }
    return newFile
}

internal fun uniqueNameIn(
    parent: DocumentFile,
    desiredName: String,
): String {
    if (parent.findFile(desiredName) == null) return desiredName
    val dotIndex = desiredName.lastIndexOf('.')
    val base = if (dotIndex > 0) desiredName.substring(0, dotIndex) else desiredName
    val extension = if (dotIndex > 0) desiredName.substring(dotIndex) else ""
    var attempt = 1
    var candidate = "$base ($attempt)$extension"
    while (parent.findFile(candidate) != null) {
        attempt++
        candidate = "$base ($attempt)$extension"
    }
    return candidate
}
