package com.efm.filemanager.data.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuditHashChainTest {
    @Test
    fun `same inputs always produce the same hash`() {
        val content = AuditEventContent(1L, "DELETE", "a.txt", "content://a", null, true)
        assertEquals(AuditHashChain.computeHash(null, content), AuditHashChain.computeHash(null, content))
    }

    @Test
    fun `changing any single field changes the hash`() {
        val base = AuditEventContent(1L, "DELETE", "a.txt", "content://a", "detail", true)
        val baseHash = AuditHashChain.computeHash("prev", base)
        assertNotEquals(baseHash, AuditHashChain.computeHash("other", base))
        assertNotEquals(baseHash, AuditHashChain.computeHash("prev", base.copy(timestamp = 2L)))
        assertNotEquals(baseHash, AuditHashChain.computeHash("prev", base.copy(action = "RENAME")))
        assertNotEquals(baseHash, AuditHashChain.computeHash("prev", base.copy(targetName = "b.txt")))
        assertNotEquals(baseHash, AuditHashChain.computeHash("prev", base.copy(targetUri = "content://b")))
        assertNotEquals(baseHash, AuditHashChain.computeHash("prev", base.copy(detail = "other")))
        assertNotEquals(baseHash, AuditHashChain.computeHash("prev", base.copy(success = false)))
    }

    @Test
    fun `an intact chain has no broken link`() {
        val first = entry(id = 1, previousHash = null)
        val second = entry(id = 2, previousHash = first.hash)
        assertNull(AuditHashChain.findFirstBrokenLink(listOf(first, second)))
    }

    @Test
    fun `a tampered field is detected as a broken link at that entry`() {
        val first = entry(id = 1, previousHash = null)
        val second = entry(id = 2, previousHash = first.hash)
        val tampered = second.copy(targetName = "tampered.txt")
        assertEquals(tampered, AuditHashChain.findFirstBrokenLink(listOf(first, tampered)))
    }

    @Test
    fun `a deleted middle entry breaks the chain at the next surviving entry`() {
        val first = entry(id = 1, previousHash = null)
        val second = entry(id = 2, previousHash = first.hash)
        val third = entry(id = 3, previousHash = second.hash)
        assertEquals(third, AuditHashChain.findFirstBrokenLink(listOf(first, third)))
    }

    private fun entry(
        id: Long,
        previousHash: String?,
    ): AuditEventEntity {
        val content = AuditEventContent(id, "DELETE", "file-$id.txt", "content://file-$id", null, true)
        return AuditEventEntity(
            id = id,
            timestamp = content.timestamp,
            action = content.action,
            targetName = content.targetName,
            targetUri = content.targetUri,
            detail = content.detail,
            success = content.success,
            previousHash = previousHash,
            hash = AuditHashChain.computeHash(previousHash, content),
        )
    }
}
