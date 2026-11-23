package com.efm.filemanager.data.share

import android.content.Intent
import androidx.core.net.toUri

private const val WHATSAPP_PACKAGE = "com.whatsapp"

/**
 * Sharing the app itself (not a file) -- distinct from [ShareIntentFactory], which shares one or
 * more files already under a granted tree. Targets WhatsApp/email specifically rather than a
 * generic chooser, matching the two explicit buttons the user asked for; the caller falls back to
 * a chooser if the targeted app isn't installed ([Intent.setPackage] makes `resolveActivity`
 * return null in that case, which this factory can't check itself without a `Context`).
 */
object AppShareFactory {
    fun whatsAppShareIntent(shareText: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            setPackage(WHATSAPP_PACKAGE)
        }

    fun chooserShareIntent(shareText: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

    fun emailShareIntent(
        subject: String,
        body: String,
    ): Intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
}
