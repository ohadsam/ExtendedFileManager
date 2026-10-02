package com.efm.filemanager.ui.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.AppearanceMode
import com.efm.filemanager.domain.model.ViewMode
import com.efm.filemanager.ui.components.InfoButton
import com.efm.filemanager.ui.components.labelRes

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenHelp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshPermissionsStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingsBody(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            viewModel = viewModel,
            onOpenLogs = onOpenLogs,
            onOpenAudit = onOpenAudit,
            onOpenHelp = onOpenHelp,
        )
    }
}

@Composable
private fun SettingsBody(
    modifier: Modifier,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onOpenLogs: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        AppearanceSection(
            uiState = uiState,
            onAppearanceModeChange = viewModel::setAppearanceMode,
            onDynamicColorChange = viewModel::setDynamicColorEnabled,
        )
        HorizontalDivider()
        LanguageSection(
            languageOption = viewModel.currentLanguageOption(),
            onLanguageChange = viewModel::setLanguage,
        )
        HorizontalDivider()
        PermissionsSection(
            grantedFolderCount = uiState.grantedFolderCount,
            onOpenSystemSettings = { openAppSystemSettings(context) },
        )
        HorizontalDivider()
        DisplaySection(
            viewMode = uiState.viewMode,
            onViewModeChange = viewModel::setViewMode,
        )
        HorizontalDivider()
        StorageAdvisorSettingsSection(
            minSizeMb = uiState.advisorMinSizeMb,
            unusedMonths = uiState.advisorUnusedMonths,
            onMinSizeChange = viewModel::setAdvisorMinSizeMb,
            onUnusedMonthsChange = viewModel::setAdvisorUnusedMonths,
        )
        HorizontalDivider()
        LogsSection(onOpenLogs = onOpenLogs)
        HorizontalDivider()
        AuditSection(onOpenAudit = onOpenAudit)
        HorizontalDivider()
        HelpSection(onOpenHelp = onOpenHelp)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    infoDescription: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (infoDescription != null) {
            InfoButton(title = title, description = infoDescription)
        }
    }
}

@Composable
private fun RadioOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun AppearanceSection(
    uiState: SettingsUiState,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.appearance_section),
            infoDescription = stringResource(R.string.appearance_info_body),
        )
        RadioOptionRow(
            label = stringResource(R.string.appearance_light),
            selected = uiState.appearanceMode == AppearanceMode.LIGHT,
            onClick = { onAppearanceModeChange(AppearanceMode.LIGHT) },
        )
        RadioOptionRow(
            label = stringResource(R.string.appearance_dark),
            selected = uiState.appearanceMode == AppearanceMode.DARK,
            onClick = { onAppearanceModeChange(AppearanceMode.DARK) },
        )
        RadioOptionRow(
            label = stringResource(R.string.appearance_system),
            selected = uiState.appearanceMode == AppearanceMode.SYSTEM,
            onClick = { onAppearanceModeChange(AppearanceMode.SYSTEM) },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.appearance_dynamic_color), modifier = Modifier.weight(1f))
                Switch(checked = uiState.dynamicColorEnabled, onCheckedChange = onDynamicColorChange)
            }
        }
    }
}

@Composable
private fun LanguageSection(
    languageOption: LanguageOption,
    onLanguageChange: (LanguageOption) -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.language_section),
            infoDescription = stringResource(R.string.language_info_body),
        )
        RadioOptionRow(
            label = stringResource(R.string.language_system),
            selected = languageOption == LanguageOption.SYSTEM,
            onClick = { onLanguageChange(LanguageOption.SYSTEM) },
        )
        RadioOptionRow(
            label = stringResource(R.string.language_english),
            selected = languageOption == LanguageOption.ENGLISH,
            onClick = { onLanguageChange(LanguageOption.ENGLISH) },
        )
        RadioOptionRow(
            label = stringResource(R.string.language_hebrew),
            selected = languageOption == LanguageOption.HEBREW,
            onClick = { onLanguageChange(LanguageOption.HEBREW) },
        )
    }
}

@Composable
private fun DisplaySection(
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
) {
    Column {
        SectionHeader(title = stringResource(R.string.display_section))
        ViewMode.entries.forEach { mode ->
            RadioOptionRow(
                label = stringResource(mode.labelRes()),
                selected = viewMode == mode,
                onClick = { onViewModeChange(mode) },
            )
        }
    }
}

private val ADVISOR_MIN_SIZE_PRESETS_MB = listOf(50, 100, 250, 500)
private val ADVISOR_UNUSED_PRESETS_MONTHS = listOf(3, 6, 12)

@Composable
private fun StorageAdvisorSettingsSection(
    minSizeMb: Int,
    unusedMonths: Int,
    onMinSizeChange: (Int) -> Unit,
    onUnusedMonthsChange: (Int) -> Unit,
) {
    Column {
        SectionHeader(title = stringResource(R.string.storage_advisor_settings_section))
        Text(
            text = stringResource(R.string.storage_advisor_settings_min_size),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ADVISOR_MIN_SIZE_PRESETS_MB.forEach { mb ->
            RadioOptionRow(
                label = stringResource(R.string.storage_advisor_size_mb, mb),
                selected = minSizeMb == mb,
                onClick = { onMinSizeChange(mb) },
            )
        }
        Text(
            text = stringResource(R.string.storage_advisor_settings_unused_duration),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ADVISOR_UNUSED_PRESETS_MONTHS.forEach { months ->
            RadioOptionRow(
                label = stringResource(R.string.storage_advisor_unused_months, months),
                selected = unusedMonths == months,
                onClick = { onUnusedMonthsChange(months) },
            )
        }
    }
}

@Composable
private fun PermissionsSection(
    grantedFolderCount: Int,
    onOpenSystemSettings: () -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.permissions_section),
            infoDescription = stringResource(R.string.permissions_protected_paths_explanation),
        )
        Text(
            text = stringResource(R.string.permissions_granted_count, grantedFolderCount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TextButton(
            onClick = onOpenSystemSettings,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(stringResource(R.string.permissions_open_system_settings))
        }
    }
}

@Composable
private fun LogsSection(onOpenLogs: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLogs)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(stringResource(R.string.logs_section), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.logs_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AuditSection(onOpenAudit: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAudit)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(stringResource(R.string.audit_section), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.audit_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HelpSection(onOpenHelp: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenHelp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(stringResource(R.string.help_section), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.help_user_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun openAppSystemSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    context.startActivity(intent)
}
