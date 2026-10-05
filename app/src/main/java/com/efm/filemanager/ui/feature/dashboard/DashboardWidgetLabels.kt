package com.efm.filemanager.ui.feature.dashboard

import com.efm.filemanager.R
import com.efm.filemanager.domain.model.DashboardTemplate
import com.efm.filemanager.domain.model.DashboardWidgetType

/** Reuses each widget's own card title string -- "Duplicates" means the same thing in the edit-mode list as it does on the card itself. */
internal fun DashboardWidgetType.titleRes(): Int =
    when (this) {
        DashboardWidgetType.STORAGE_SUMMARY -> R.string.statistics_storage_used_title
        DashboardWidgetType.DUPLICATES -> R.string.statistics_duplicates_title
        DashboardWidgetType.INSIGHTS -> R.string.statistics_advisor_title
        DashboardWidgetType.FAVORITES -> R.string.dashboard_favorites_title
    }

internal fun DashboardTemplate.labelRes(): Int =
    when (this) {
        DashboardTemplate.CLEANUP_FOCUS -> R.string.dashboard_template_cleanup_focus
        DashboardTemplate.QUICK_ACCESS -> R.string.dashboard_template_quick_access
        DashboardTemplate.AT_A_GLANCE -> R.string.dashboard_template_at_a_glance
    }
