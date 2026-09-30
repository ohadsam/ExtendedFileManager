package com.efm.filemanager.data.vault

import android.content.Context
import android.net.Uri
import com.efm.filemanager.data.audit.AuditAction
import com.efm.filemanager.data.audit.AuditLogger
import com.efm.filemanager.data.documenttree.FileOperationsRepository
import com.efm.filemanager.data.documenttree.LockedFileException
import com.efm.filemanager.data.documenttree.requireTreeDocument
import com.efm.filemanager.data.local.VaultEntryDao
import com.efm.filemanager.data.local.VaultEntryEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.data.metadata.FileFlagsRepository
import com.efm.filemanager.data.security.PathGuard
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.VaultEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

private const val VAULT_DIR_NAME = "vault"
private const val DEFAULT_MIME_TYPE = "application/octet-stream"

/**
 * Moves a file into the encrypted vault (see [VaultFileCrypto]) and back out again -- "move", not
 * "copy": a vaulted file is meant to exist only there, encrypted, until explicitly exported.
 * Removing a vault entry deletes its encrypted bytes directly, never through the shared trash --
 * the vault exists precisely to keep this content out of ordinary, discoverable storage, so a
 * recoverable trash copy sitting in plain SAF storage would defeat that. See docs/PLAN.md Phase 11.
 */
class VaultRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val vaultEntryDao: VaultEntryDao,
        private val vaultFileCrypto: VaultFileCrypto,
        private val fileOperationsRepository: FileOperationsRepository,
        private val fileFlagsRepository: FileFlagsRepository,
        private val auditLogger: AuditLogger,
    ) {
        fun observeEntries(): Flow<List<VaultEntry>> = vaultEntryDao.observeAll().map { entries -> entries.map { it.toDomain() } }

        /**
         * Encrypts [entry]'s bytes into the vault first, then deletes the original via the
         * normal, lock-and-protected-path-guarded delete path -- in that order, so a failure
         * partway through never loses the original before a safe encrypted copy exists.
         */
        suspend fun addToVault(
            entry: FileEntry,
            parentUri: Uri,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                if (fileFlagsRepository.isLocked(entry.uri)) {
                    auditLogger.log(AuditAction.ADD_TO_VAULT, entry.name, entry.uri, success = false, detail = "locked")
                    Result.failure(LockedFileException(entry.name))
                } else {
                    runCatching { encryptIntoVault(entry, parentUri) }
                        .onSuccess { auditLogger.log(AuditAction.ADD_TO_VAULT, entry.name, entry.uri, success = true) }
                        .onFailure {
                            auditLogger.log(AuditAction.ADD_TO_VAULT, entry.name, entry.uri, success = false, detail = it.message)
                        }
                }
            }

        private suspend fun encryptIntoVault(
            entry: FileEntry,
            parentUri: Uri,
        ) {
            val encryptedFileName = "${UUID.randomUUID()}.enc"
            val source =
                requireNotNull(context.contentResolver.openInputStream(entry.uri)) { "Could not read ${entry.name}" }
            source.use { vaultFileCrypto.encrypt(it, File(vaultDir(), encryptedFileName)) }
            vaultEntryDao.insert(
                VaultEntryEntity(
                    encryptedFileName = encryptedFileName,
                    originalName = entry.name,
                    mimeType = entry.mimeType,
                    size = entry.size,
                    addedAt = System.currentTimeMillis(),
                ),
            )
            fileOperationsRepository.delete(entry, parentUri).getOrThrow()
        }

        /** Decrypts a vault entry back out to [targetParentUri] via SAF, then removes it from the vault. */
        suspend fun exportFromVault(
            id: Long,
            targetParentUri: Uri,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                val stored = vaultEntryDao.getById(id)
                val vaultedName = stored?.originalName.orEmpty()
                val result =
                    if (stored == null) {
                        Result.failure(IllegalStateException("Not in the vault"))
                    } else {
                        runCatching { decryptOutOfVault(stored, targetParentUri) }
                    }
                result
                    .onSuccess { auditLogger.log(AuditAction.EXPORT_FROM_VAULT, vaultedName, targetParentUri, success = true) }
                    .onFailure {
                        auditLogger.log(AuditAction.EXPORT_FROM_VAULT, vaultedName, targetParentUri, success = false, detail = it.message)
                    }
            }

        private fun decryptOutOfVault(
            stored: VaultEntryEntity,
            targetParentUri: Uri,
        ) {
            PathGuard.checkAllowed(targetParentUri)
            val targetParent = requireTreeDocument(context, targetParentUri)
            val mimeType = stored.mimeType ?: DEFAULT_MIME_TYPE
            val target =
                requireNotNull(targetParent.createFile(mimeType, stored.originalName)) { "Could not export ${stored.originalName}" }
            vaultFileCrypto.decrypt(encryptedFile(stored.encryptedFileName)).use { input ->
                context.contentResolver.openOutputStream(target.uri)?.use { output -> input.copyTo(output) }
            }
            encryptedFile(stored.encryptedFileName).delete()
            vaultEntryDao.delete(stored.id)
        }

        /** Permanently deletes a vault entry's encrypted bytes and its Room row. */
        suspend fun removeFromVault(id: Long): Result<Unit> =
            withContext(Dispatchers.IO) {
                val stored = vaultEntryDao.getById(id)
                val vaultedName = stored?.originalName.orEmpty()
                val result =
                    if (stored == null) {
                        Result.failure(IllegalStateException("Not in the vault"))
                    } else {
                        runCatching {
                            encryptedFile(stored.encryptedFileName).delete()
                            vaultEntryDao.delete(id)
                        }
                    }
                result
                    .onSuccess { auditLogger.log(AuditAction.REMOVE_FROM_VAULT, vaultedName, Uri.EMPTY, success = true) }
                    .onFailure {
                        auditLogger.log(AuditAction.REMOVE_FROM_VAULT, vaultedName, Uri.EMPTY, success = false, detail = it.message)
                    }
            }

        private fun vaultDir(): File = File(context.filesDir, VAULT_DIR_NAME).apply { mkdirs() }

        private fun encryptedFile(name: String): File = File(vaultDir(), name)
    }
