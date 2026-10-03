package com.efm.filemanager.data.share

import android.content.Intent
import com.efm.filemanager.domain.model.FileEntry

private const val WILDCARD_MIME_TYPE = "*/*"

/**
 * Builds a system share-sheet `Intent` for one or more files, each already a `content://` SAF
 * `Uri` -- no `FileProvider` needed, since these URIs are already backed by a content provider.
 * This is deliberately just the OS-native share intent (any installed app -- Drive, Dropbox,
 * Gmail, etc. -- can register to receive it), not a direct cloud-provider integration: a real
 * Drive/Dropbox SDK integration needs OAuth credentials and a way to exercise the flow, neither
 * of which exists yet (see docs/PLAN.md Phase 16).
 */
object ShareIntentFactory {
    fun createShareIntent(entries: List<FileEntry>): Intent {
        require(entries.isNotEmpty()) { "Nothing to share" }
        val mimeType = commonMimeType(entries)
        return if (entries.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, entries.first().uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(entries.map { it.uri }))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}

/** The shared mime type if every entry agrees, otherwise the wildcard -- a receiving app decides what it can handle either way. */
internal fun commonMimeType(entries: List<FileEntry>): String {
    val mimeTypes = entries.map { it.mimeType ?: WILDCARD_MIME_TYPE }.distinct()
    return mimeTypes.singleOrNull() ?: WILDCARD_MIME_TYPE
}
