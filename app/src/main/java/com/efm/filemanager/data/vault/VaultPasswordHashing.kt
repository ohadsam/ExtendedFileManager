package com.efm.filemanager.data.vault

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val SALT_LENGTH_BYTES = 16
private const val PBKDF2_ITERATIONS = 120_000
private const val KEY_LENGTH_BITS = 256

/** A fresh, random salt for [hashPassword] -- one per password, never reused. */
internal fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }

/**
 * PBKDF2-WithHmacSHA256 over [password] and [salt] -- deliberately slow (120k iterations) so a
 * stolen hash is expensive to brute-force. This is only ever the vault's app-level password
 * verifier, never the file-encryption key itself -- see [VaultFileCrypto], which is Keystore-
 * backed and doesn't derive from this password at all, so a forgotten password never makes
 * already-vaulted files unrecoverable by design (only re-settable by clearing the vault).
 */
internal fun hashPassword(
    password: CharArray,
    salt: ByteArray,
): ByteArray {
    val spec: KeySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
}

/** Compares two hashes in constant time, so verification timing can't leak how much of a guess matched. */
internal fun constantTimeEquals(
    a: ByteArray,
    b: ByteArray,
): Boolean {
    if (a.size != b.size) return false
    var result = 0
    for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
    return result == 0
}
