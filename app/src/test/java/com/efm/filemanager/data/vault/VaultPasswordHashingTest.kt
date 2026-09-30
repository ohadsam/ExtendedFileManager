package com.efm.filemanager.data.vault

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultPasswordHashingTest {
    @Test
    fun `hashing the same password with the same salt is deterministic`() {
        val salt = generateSalt()
        val hash1 = hashPassword("correct horse".toCharArray(), salt)
        val hash2 = hashPassword("correct horse".toCharArray(), salt)
        assertTrue(constantTimeEquals(hash1, hash2))
    }

    @Test
    fun `a different password produces a different hash`() {
        val salt = generateSalt()
        val hash1 = hashPassword("correct horse".toCharArray(), salt)
        val hash2 = hashPassword("wrong horse".toCharArray(), salt)
        assertFalse(constantTimeEquals(hash1, hash2))
    }

    @Test
    fun `the same password with a different salt produces a different hash`() {
        val hash1 = hashPassword("correct horse".toCharArray(), generateSalt())
        val hash2 = hashPassword("correct horse".toCharArray(), generateSalt())
        assertFalse(constantTimeEquals(hash1, hash2))
    }

    @Test
    fun `constantTimeEquals rejects different lengths without throwing`() {
        assertFalse(constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2)))
    }

    @Test
    fun `generateSalt produces a fresh value each call`() {
        assertFalse(generateSalt().contentEquals(generateSalt()))
    }
}
