package com.efm.filemanager.data.vault

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val VAULT_AUTH_MASTER_KEY_ALIAS = "efm_vault_auth_master_key"
private const val VAULT_AUTH_PREFS_NAME = "efm_vault_auth"
private const val KEY_PASSWORD_HASH = "password_hash"
private const val KEY_PASSWORD_SALT = "password_salt"
private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

/**
 * Gates the Vault behind a password -- required, set on first use -- optionally backed up by
 * biometric unlock once a password exists. The password itself is never stored, only a salted
 * PBKDF2 hash (see [VaultPasswordHashing][hashPassword]); hash, salt, and the biometric
 * preference live in [EncryptedSharedPreferences] rather than plain DataStore, since this is
 * more sensitive than an ordinary app setting. See docs/PLAN.md Phase 11.
 */
@Singleton
class VaultAuthRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val prefs by lazy {
            val masterKey = MasterKey.Builder(context, VAULT_AUTH_MASTER_KEY_ALIAS).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context,
                VAULT_AUTH_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        fun isPasswordSet(): Boolean = prefs.contains(KEY_PASSWORD_HASH)

        var biometricEnabled: Boolean
            get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
            set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

        suspend fun setPassword(password: CharArray) =
            withContext(Dispatchers.IO) {
                val salt = generateSalt()
                val hash = hashPassword(password, salt)
                prefs.edit()
                    .putString(KEY_PASSWORD_HASH, hash.toBase64())
                    .putString(KEY_PASSWORD_SALT, salt.toBase64())
                    .apply()
            }

        suspend fun verifyPassword(password: CharArray): Boolean =
            withContext(Dispatchers.IO) {
                val storedHash = storedBytes(KEY_PASSWORD_HASH)
                val salt = storedBytes(KEY_PASSWORD_SALT)
                storedHash != null && salt != null && constantTimeEquals(hashPassword(password, salt), storedHash)
            }

        /** Forgets the password, salt, and biometric preference -- the vault's own encrypted files are untouched. */
        suspend fun clearPassword() =
            withContext(Dispatchers.IO) {
                prefs.edit()
                    .remove(KEY_PASSWORD_HASH)
                    .remove(KEY_PASSWORD_SALT)
                    .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                    .apply()
            }

        private fun storedBytes(key: String): ByteArray? = prefs.getString(key, null)?.let(::fromBase64)
    }

private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

private fun fromBase64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
