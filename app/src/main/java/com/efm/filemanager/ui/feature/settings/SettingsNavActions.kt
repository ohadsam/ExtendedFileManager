package com.efm.filemanager.ui.feature.settings

/** [SettingsScreen]'s cross-screen navigation callbacks, bundled to keep that composable's own arity down. */
data class SettingsNavActions(
    val onOpenLogs: () -> Unit,
    val onOpenAudit: () -> Unit,
    val onOpenHelp: () -> Unit,
)
