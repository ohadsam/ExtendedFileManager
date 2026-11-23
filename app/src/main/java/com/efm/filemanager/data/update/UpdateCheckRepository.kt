package com.efm.filemanager.data.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

private const val RELEASES_API_URL = "https://api.github.com/repos/ohadsam/ExtendedFileManager/releases/latest"
private const val USER_AGENT = "ExtendedFileManager-UpdateChecker"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 10_000
private const val APK_SUFFIX = ".apk"

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult

    data class UpdateAvailable(
        val versionName: String,
        val releaseNotes: String,
        val apkDownloadUrl: String?,
        val apkFileName: String,
    ) : UpdateCheckResult

    data class Error(val message: String) : UpdateCheckResult
}

/**
 * EFM's only network call -- everything else in this app works entirely offline against
 * SAF-granted trees. Deliberately opt-in only: Settings' "Check for updates" button is the one
 * and only place this ever runs, never an automatic/background check, so a file manager built
 * around user-granted access never phones home without being explicitly asked to, right now.
 * GitHub's public Releases API needs no auth token for a public repo's GET, just a `User-Agent`
 * header (requests without one are rejected outright).
 */
class UpdateCheckRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun checkForUpdate(): UpdateCheckResult =
            withContext(Dispatchers.IO) {
                runCatching { fetchLatestRelease() }
                    .fold(
                        onSuccess = { release -> release.toCheckResult(currentVersionName()) },
                        onFailure = { error -> UpdateCheckResult.Error(error.message ?: error::class.simpleName.orEmpty()) },
                    )
            }

        private fun fetchLatestRelease(): JSONObject {
            val connection = URL(RELEASES_API_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
                return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }

        private fun currentVersionName(): String {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionName ?: ""
        }
    }

private fun JSONObject.toCheckResult(currentVersion: String): UpdateCheckResult {
    val tagName = optString("tag_name", "")
    if (tagName.isBlank() || !isNewerVersion(tagName, currentVersion)) return UpdateCheckResult.UpToDate
    val assets =
        optJSONArray("assets")?.let { array ->
            (0 until array.length()).map { index ->
                val asset = array.getJSONObject(index)
                asset.optString("name", "") to asset.optString("browser_download_url", "")
            }
        } ?: emptyList()
    val apkAsset = findApkAsset(assets)
    return UpdateCheckResult.UpdateAvailable(
        versionName = tagName,
        releaseNotes = optString("body", ""),
        apkDownloadUrl = apkAsset?.second,
        apkFileName = apkAsset?.first ?: "update.apk",
    )
}

/** First asset ending in [APK_SUFFIX] -- a release with no APK attached (source-only) yields null, "nothing to download" upstream. */
internal fun findApkAsset(assets: List<Pair<String, String>>): Pair<String, String>? =
    assets.firstOrNull { (name, _) -> name.endsWith(APK_SUFFIX) }

/**
 * Compares dotted version numbers component-by-component, a missing trailing component treated
 * as zero -- "1.2" == "1.2.0". A leading "v" (GitHub's own tag convention) is stripped from either side.
 */
internal fun isNewerVersion(
    remoteVersion: String,
    currentVersion: String,
): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    val current = currentVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    for (index in 0 until maxOf(remote.size, current.size)) {
        val remotePart = remote.getOrElse(index) { 0 }
        val currentPart = current.getOrElse(index) { 0 }
        if (remotePart != currentPart) return remotePart > currentPart
    }
    return false
}
