package com.efm.filemanager.data.vault

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val VAULT_FILE_MASTER_KEY_ALIAS = "efm_vault_file_master_key"

/**
 * Encrypts/decrypts vault file contents at rest via a single, Keystore-backed [MasterKey] --
 * hardware-backed where the device supports it, with no per-use system auth prompt of its own.
 * [VaultAuthRepository]'s password/biometric gate is what protects *access* to the vault for the
 * rest of an app session; this is what protects the bytes themselves even if someone got at the
 * app's private storage directly. See docs/PLAN.md Phase 11.
 */
@Singleton
class VaultFileCrypto
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val masterKey: MasterKey by lazy {
            MasterKey.Builder(context, VAULT_FILE_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }

        /** Encrypts [source]'s bytes into a brand-new [destination] file, which must not already exist. */
        fun encrypt(
            source: InputStream,
            destination: File,
        ) {
            encryptedFile(destination).openFileOutput().use { output -> source.copyTo(output) }
        }

        /** Opens [source] (a file previously written by [encrypt]) for streamed, decrypted reading. */
        fun decrypt(source: File): InputStream = encryptedFile(source).openFileInput()

        private fun encryptedFile(file: File): EncryptedFile =
            EncryptedFile.Builder(context, file, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
    }
