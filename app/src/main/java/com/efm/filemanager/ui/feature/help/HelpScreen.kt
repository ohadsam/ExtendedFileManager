package com.efm.filemanager.ui.feature.help

import android.webkit.WebView
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.efm.filemanager.R

@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    val assetPath = if (LocalConfiguration.current.locales[0].language == "he") "help_he.html" else "help_en.html"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_section)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        HelpWebView(assetPath = assetPath, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun HelpWebView(
    assetPath: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> WebView(context) },
        update = { webView -> webView.loadUrl("file:///android_asset/$assetPath") },
    )
}
