package com.efm.filemanager.ui.feature.dashboard

internal sealed interface DashboardDialog {
    data object SaveLayout : DashboardDialog
}
