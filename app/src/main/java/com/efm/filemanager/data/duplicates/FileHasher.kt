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
    var read = readChunk(input, buffer, byteLimit, totalRead)
    while (read > 0) {
        digest.update(buffer, 0, read)
        totalRead += read
        read = readChunk(input, buffer, byteLimit, totalRead)
    }
}

/** Reads the next chunk, or -1 once [byteLimit] (if any) has been reached -- same sentinel [InputStream.read] itself uses for EOF. */
private fun readChunk(
    input: InputStream,
    buffer: ByteArray,
    byteLimit: Int?,
    totalRead: Int,
): Int {
    val remaining = byteLimit?.let { limit -> limit - totalRead } ?: buffer.size
    if (remaining <= 0) return -1
    return input.read(buffer, 0, minOf(buffer.size, remaining))
}
