package com.efm.filemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One file held in the encrypted vault. [encryptedFileName] is the opaque name of its encrypted
 * blob under the app-private vault directory (see [com.efm.filemanager.data.vault.VaultFileCrypto]);
 * [size]/[mimeType] describe the original, unencrypted file, for display only.
 */
@Entity(tableName = "vault_entries")
data class VaultEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedFileName: String,
    val originalName: String,
    val mimeType: String?,
    val size: Long,
    val addedAt: Long,
)
