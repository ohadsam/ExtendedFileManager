package com.efm.filemanager.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Phase 15's original spec called for "APK signature verification before install prompt" -- this
 * is that check. An update downloaded from GitHub Releases is just a file this app fetched over
 * plain HTTP(S); nothing stops a compromised release, a MITM, or a stale CDN cache from serving a
 * different APK than the one actually published. Comparing the downloaded file's own signing
 * certificate against this already-installed app's certificate (the same comparison
 * `PackageManager` itself makes before silently allowing an update-in-place) means
 * [UpdateDownloadRepository] can refuse the install prompt entirely on a mismatch, rather than
 * trusting the download at face value.
 */
class ApkSignatureVerifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun isSignedBySameCertificate(apkFile: File): Boolean {
            val installed = installedAppSigningHashes()
            val downloaded = apkFileSigningHashes(apkFile)
            return installed.isNotEmpty() && installed == downloaded
        }

        private fun installedAppSigningHashes(): Set<String> {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, signingInfoFlags())
            return packageInfo.signingCertificateHashes()
        }

        private fun apkFileSigningHashes(apkFile: File): Set<String> {
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, signingInfoFlags()) ?: return emptySet()
            return packageInfo.signingCertificateHashes()
        }
    }

private fun signingInfoFlags(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

@Suppress("DEPRECATION")
private fun PackageInfo.rawSignatures(): Array<Signature> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = signingInfo
        when {
            info == null -> emptyArray()
            info.hasMultipleSigners() -> info.apkContentsSigners
            else -> info.signingCertificateHistory
        }
    } else {
        signatures ?: emptyArray()
    }

private fun PackageInfo.signingCertificateHashes(): Set<String> {
    val digest = MessageDigest.getInstance("SHA-256")
    return rawSignatures().map { signature -> digest.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
}
