package com.efm.filemanager.ui.feature.globalfiles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.GlobalFilesSort
import com.efm.filemanager.ui.components.FileRow
import com.efm.filemanager.ui.components.query.labelRes

@Composable
fun GlobalFileListScreen(
    onNavigateBack: () -> Unit,
    onOpenPreview: () -> Unit,
    viewModel: GlobalFileListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { GlobalFileListTopBar(category = uiState.category, sort = uiState.sort, onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        GlobalFileListBody(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            onFileClick = { entry ->
                viewModel.openPreview(entry)
                onOpenPreview()
            },
        )
    }
}

@Composable
private fun GlobalFileListTopBar(
    category: FileCategory?,
    sort: GlobalFilesSort,
    onNavigateBack: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
        },
        title = { Text(globalFileListTitle(category, sort)) },
    )
}

/** [category] always wins when set; an uncategorized list falls back to whichever widget's sort it drilled down from. */
@Composable
private fun globalFileListTitle(
    category: FileCategory?,
    sort: GlobalFilesSort,
): String =
    when {
        category != null -> stringResource(category.labelRes())
        sort == GlobalFilesSort.RECENT -> stringResource(R.string.statistics_recently_modified_title)
        else -> stringResource(R.string.statistics_largest_files_title)
    }

@Composable
private fun GlobalFileListBody(
    modifier: Modifier,
    uiState: GlobalFileListUiState,
    onFileClick: (FileEntry) -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (uiState.files.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.statistics_empty))
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(uiState.files, key = { it.uri.toString() }) { entry ->
            FileRow(entry = entry, isSelected = false, onClick = { onFileClick(entry) }, onLongClick = {})
        }
    }
}
