package com.efm.filemanager.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.domain.model.AppearanceMode
import com.efm.filemanager.ui.theme.ExtendedFileManagerTheme

@Composable
fun EfmRoot(
    initialDestinationRoute: String? = null,
    viewModel: AppThemeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val darkTheme =
        when (uiState.appearanceMode) {
            AppearanceMode.LIGHT -> false
            AppearanceMode.DARK -> true
            AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        }
    ExtendedFileManagerTheme(darkTheme = darkTheme, dynamicColor = uiState.dynamicColorEnabled) {
        EfmApp(initialDestinationRoute = initialDestinationRoute)
    }
}
