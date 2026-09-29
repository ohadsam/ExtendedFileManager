package com.efm.filemanager.ui.feature.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R

@Composable
fun PreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val current = session
    if (current == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
    } else {
        PreviewContent(session = current, onNavigateBack = onNavigateBack)
    }
}

@Composable
private fun PreviewContent(
    session: PreviewSession,
    onNavigateBack: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = session.startIndex) { session.entries.size }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                title = {
                    Text(
                        text = session.entries.getOrNull(pagerState.currentPage)?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { page -> PreviewPage(entry = session.entries[page]) }
    }
}
