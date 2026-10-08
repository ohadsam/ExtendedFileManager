package com.efm.filemanager.ui.feature.dashboard

import com.efm.filemanager.data.dashboard.DashboardSavedLayout

internal sealed interface DashboardDialog {
    data object SaveLayout : DashboardDialog

    data class RenameLayout(val layout: DashboardSavedLayout) : DashboardDialog

    data class DeleteLayout(val layout: DashboardSavedLayout) : DashboardDialog
}
