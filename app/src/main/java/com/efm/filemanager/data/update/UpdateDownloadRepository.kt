package com.efm.filemanager.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/**
 * Downloads the update APK through the system [DownloadManager] (its own progress notification,
 * survives the app leaving the foreground) rather than a raw HTTP stream -- the same "let the OS
 * own a long transfer" principle Phase 16's upload worker already follows, just via the
 * platform's download queue instead of WorkManager, since this is a one-shot user-initiated fetch
 * of a single file, not something that needs its own retry/backoff bookkeeping. The destination
 * is always this exact app-private path (never a public Downloads folder another app could
 * tamper with between download and install), matching `res/xml/file_paths.xml`'s own entry.
 */
class UpdateDownloadRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val apkSignatureVerifier: ApkSignatureVerifier,
    ) {
        private val downloadManager: DownloadManager
            get() = context.getSystemService(DownloadManager::class.java)

        fun enqueueDownload(
            url: String,
            fileName: String,
        ): Long {
            val request =
                DownloadManager.Request(url.toUri())
                    .setTitle(fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setMimeType(APK_MIME_TYPE)
            return downloadManager.enqueue(request)
        }

        fun downloadStatus(downloadId: Long): Int {
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            return cursor.use { result ->
                if (!result.moveToFirst()) return@use DownloadManager.STATUS_FAILED
                result.getInt(result.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            }
        }

        /** Null on a signature mismatch -- never offers to install an APK not signed with this app's own certificate, download aside. */
        fun verifiedInstallIntent(fileName: String): Intent? {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (!file.exists() || !apkSignatureVerifier.isSignedBySameCertificate(file)) return null
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            return Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
