package com.efm.filemanager.ui.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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
    navActions: SettingsNavActions,
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
            navActions = navActions,
        )
    }
}

@Composable
private fun SettingsBody(
    modifier: Modifier,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    navActions: SettingsNavActions,
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
            settings = uiState.advisorSettings,
            callbacks =
                AdvisorSettingsCallbacks(
                    onMinSizeChange = viewModel::setAdvisorMinSizeMb,
                    onUnusedMonthsChange = viewModel::setAdvisorUnusedMonths,
                    onStagedReviewDaysChange = viewModel::setStagedReviewDays,
                ),
        )
        HorizontalDivider()
        InsightsSettingsSectionHost(
            insightsSettings = uiState.insightsSettings,
            onRunDailyInsightsChange = viewModel::setRunDailyInsights,
            onNotifyMeEnabledChange = viewModel::setNotifyMeEnabled,
        )
        HorizontalDivider()
        LogsSection(onOpenLogs = navActions.onOpenLogs)
        HorizontalDivider()
        AuditSection(onOpenAudit = navActions.onOpenAudit)
        HorizontalDivider()
        HelpSection(onOpenHelp = navActions.onOpenHelp)
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
private val STAGED_REVIEW_PRESETS_DAYS = listOf(7, 14, 30, 60)

/** Bundled so [StorageAdvisorSettingsSection] stays under detekt's `LongParameterList` threshold. */
private data class AdvisorSettingsCallbacks(
    val onMinSizeChange: (Int) -> Unit,
    val onUnusedMonthsChange: (Int) -> Unit,
    val onStagedReviewDaysChange: (Int) -> Unit,
)

@Composable
private fun StorageAdvisorSettingsSection(
    settings: StorageAdvisorSettingsState,
    callbacks: AdvisorSettingsCallbacks,
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
                selected = settings.minSizeMb == mb,
                onClick = { callbacks.onMinSizeChange(mb) },
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
                selected = settings.unusedMonths == months,
                onClick = { callbacks.onUnusedMonthsChange(months) },
            )
        }
        Text(
            text = stringResource(R.string.storage_advisor_settings_staged_review),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        STAGED_REVIEW_PRESETS_DAYS.forEach { days ->
            RadioOptionRow(
                label = stringResource(R.string.storage_advisor_staged_review_days, days),
                selected = settings.stagedReviewDays == days,
                onClick = { callbacks.onStagedReviewDaysChange(days) },
            )
        }
    }
}

// Hosts the permission launcher itself so SettingsBody doesn't have to -- keeps that
// function's line count from creeping toward detekt's LongMethod threshold.
@Composable
private fun InsightsSettingsSectionHost(
    insightsSettings: InsightsSettingsState,
    onRunDailyInsightsChange: (Boolean) -> Unit,
    onNotifyMeEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onNotifyMeEnabledChange(granted)
        }
    InsightsSettingsSection(
        runDailyInsights = insightsSettings.runDailyInsights,
        notifyMeEnabled = insightsSettings.notifyMeEnabled,
        onRunDailyInsightsChange = onRunDailyInsightsChange,
        onNotifyMeChange = { enabled ->
            if (needsNotificationPermission(context, enabled)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onNotifyMeEnabledChange(enabled)
            }
        },
    )
}

@Composable
private fun InsightsSettingsSection(
    runDailyInsights: Boolean,
    notifyMeEnabled: Boolean,
    onRunDailyInsightsChange: (Boolean) -> Unit,
    onNotifyMeChange: (Boolean) -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.insights_settings_section),
            infoDescription = stringResource(R.string.insights_settings_info_body),
        )
        SwitchRow(
            label = stringResource(R.string.insights_settings_run_daily),
            checked = runDailyInsights,
            onCheckedChange = onRunDailyInsightsChange,
        )
        SwitchRow(
            label = stringResource(R.string.insights_settings_notify_me),
            checked = notifyMeEnabled,
            onCheckedChange = onNotifyMeChange,
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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

/** Turning "Notify me" on needs the real runtime permission on API 33+ -- below that, or when already granted, no request is needed. */
private fun needsNotificationPermission(
    context: Context,
    enabled: Boolean,
): Boolean {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}
