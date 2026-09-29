package com.efm.filemanager.data.duplicates

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.security.MessageDigest

private const val PARTIAL_HASH_BYTES = 4096
private const val STREAM_BUFFER_SIZE = 8192

/** Hashes only the first few KB -- cheap enough to run on every same-size candidate. */
internal fun partialHash(
    context: Context,
    uri: Uri,
): String = computeHash(context, uri, PARTIAL_HASH_BYTES)

/** Full streaming SHA-256 -- only run on candidates that already passed the partial-hash check. */
internal fun fullHash(
    context: Context,
    uri: Uri,
): String = computeHash(context, uri, byteLimit = null)

private fun computeHash(
    context: Context,
    uri: Uri,
    byteLimit: Int?,
): String {
    val digest = MessageDigest.getInstance("SHA-256")
    context.contentResolver.openInputStream(uri)?.use { input -> digestStream(input, digest, byteLimit) }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

private fun digestStream(
    input: InputStream,
    digest: MessageDigest,
    byteLimit: Int?,
) {
    val buffer = ByteArray(STREAM_BUFFER_SIZE)
    var totalRead = 0
    while (true) {
        val remaining = byteLimit?.let { limit -> limit - totalRead }
        if (remaining != null && remaining <= 0) break
        val toRead = if (remaining != null) minOf(buffer.size, remaining) else buffer.size
        val read = input.read(buffer, 0, toRead)
        if (read <= 0) break
        digest.update(buffer, 0, read)
        totalRead += read
    }
}
