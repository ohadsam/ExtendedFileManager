package com.efm.filemanager.ui.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.data.share.ShareIntentFactory
import com.efm.filemanager.domain.model.FileEntry

/**
 * Launches the system share sheet once for [entries], then immediately calls [onFinished] --
 * there's no confirm step, unlike a destructive action's dialog. Shared by every screen that
 * offers "Share" from its selection menu (Phase 16's own Browse rollout, plus the screen-by-screen
 * follow-up every one of Duplicates/Favorites/Search/Preview now shares this exact composable
 * for, rather than each repeating the same `Intent.createChooser` call).
 */
@Composable
fun ShareLauncher(
    entries: List<FileEntry>,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share_chooser_title)
    LaunchedEffect(entries) {
        context.startActivity(Intent.createChooser(ShareIntentFactory.createShareIntent(entries), chooserTitle))
        onFinished()
    }
}
