package com.efm.filemanager.ui.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R
import com.efm.filemanager.data.share.AppShareFactory

private const val APP_SHARE_URL = "https://github.com/ohadsam/ExtendedFileManager"

/** Pulled out of `SettingsScreen.kt` to keep that file under detekt's `TooManyFunctions` threshold. */
@Composable
internal fun AboutSection(
    appVersionName: String,
    onCheckForUpdates: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareEmail: () -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.about_section),
            infoDescription = stringResource(R.string.about_info_body),
        )
        Text(
            text = stringResource(R.string.about_version, appVersionName),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TextButton(onClick = onCheckForUpdates, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(R.string.update_check_action))
        }
        Row {
            TextButton(onClick = onShareWhatsApp) { Text(stringResource(R.string.share_app_whatsapp_action)) }
            TextButton(onClick = onShareEmail) { Text(stringResource(R.string.share_app_email_action)) }
        }
    }
}

internal fun currentAppVersionName(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionName ?: ""
}

internal fun canInstallPackages(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

internal fun unknownAppSourcesIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }

private fun shareAppText(context: Context): String = context.getString(R.string.share_app_text, APP_SHARE_URL)

/** Targets WhatsApp specifically, falling back to the system chooser only if WhatsApp isn't installed. */
internal fun shareAppViaWhatsApp(context: Context) {
    val text = shareAppText(context)
    val whatsAppIntent = AppShareFactory.whatsAppShareIntent(text)
    val isWhatsAppInstalled = whatsAppIntent.resolveActivity(context.packageManager) != null
    context.startActivity(if (isWhatsAppInstalled) whatsAppIntent else AppShareFactory.chooserShareIntent(text))
}

/** No email client at all is rare but possible -- falls back to the chooser, same as WhatsApp's own does above. */
internal fun shareAppViaEmail(context: Context) {
    val text = shareAppText(context)
    val emailIntent = AppShareFactory.emailShareIntent(context.getString(R.string.share_app_email_subject), text)
    val canHandleEmail = emailIntent.resolveActivity(context.packageManager) != null
    context.startActivity(if (canHandleEmail) emailIntent else AppShareFactory.chooserShareIntent(text))
}
