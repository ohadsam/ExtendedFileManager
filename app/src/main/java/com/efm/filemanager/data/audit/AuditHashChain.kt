package com.efm.filemanager.data.audit

import java.security.MessageDigest

/**
 * Pure SHA-256 hash-chaining logic for the audit trail -- each entry's hash folds in the
 * previous entry's hash, so altering or deleting any past entry breaks every hash computed
 * after it, making tampering detectable without a separate signature scheme. No Android
 * dependency, so it's unit-tested directly, the same split `PathGuard`/`VaultPasswordHashing`
 * already established for this project's security-sensitive logic.
 */
/** Every field that feeds a hash-chained entry's own hash, besides the chain link itself -- see [AuditHashChain]. */
data class AuditEventContent(
    val timestamp: Long,
    val action: String,
    val targetName: String,
    val targetUri: String,
    val detail: String?,
    val success: Boolean,
)

object AuditHashChain {
    fun computeHash(
        previousHash: String?,
        content: AuditEventContent,
    ): String {
        val payload =
            listOf(
                previousHash.orEmpty(),
                content.timestamp.toString(),
                content.action,
                content.targetName,
                content.targetUri,
                content.detail.orEmpty(),
                content.success.toString(),
            ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * Walks [events] oldest-first, recomputing each hash from the one before it; returns the
     * first entry (if any) whose stored hash doesn't match, meaning the chain was broken at or
     * before that point. `null` means the whole chain verifies intact.
     */
    fun findFirstBrokenLink(events: List<AuditEventEntity>): AuditEventEntity? {
        val oldestFirst = events.sortedBy { it.id }
        var expectedPreviousHash: String? = null
        for (event in oldestFirst) {
            val content = AuditEventContent(event.timestamp, event.action, event.targetName, event.targetUri, event.detail, event.success)
            val expectedHash = computeHash(expectedPreviousHash, content)
            if (event.previousHash != expectedPreviousHash || event.hash != expectedHash) return event
            expectedPreviousHash = event.hash
        }
        return null
    }
}
