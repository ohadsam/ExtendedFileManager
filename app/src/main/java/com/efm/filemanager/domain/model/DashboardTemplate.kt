package com.efm.filemanager.domain.model

/** One widget's state within a [DashboardTemplate] -- position in [DashboardTemplate.widgets] becomes its sortOrder when applied. */
data class DashboardTemplateWidget(
    val type: DashboardWidgetType,
    val isEnabled: Boolean,
    val size: DashboardWidgetSize,
)

/**
 * Phase 19's built-in starting points -- an explicit alternative to a blank dashboard on first
 * use, not a locked layout: applying one just rewrites today's widget rows (enabled/order/size),
 * exactly as if the user had made each of those changes by hand in edit mode, so everything
 * stays freely customizable afterward. Every template must list every [DashboardWidgetType]
 * exactly once (checked in `DashboardTemplateTest`) -- applying one is a full rewrite, not a
 * partial one that could leave some widget's old state stale.
 */
enum class DashboardTemplate(val widgets: List<DashboardTemplateWidget>) {
    CLEANUP_FOCUS(
        listOf(
            DashboardTemplateWidget(DashboardWidgetType.DUPLICATES, isEnabled = true, size = DashboardWidgetSize.DETAILED),
            DashboardTemplateWidget(DashboardWidgetType.INSIGHTS, isEnabled = true, size = DashboardWidgetSize.DETAILED),
            DashboardTemplateWidget(DashboardWidgetType.STORAGE_SUMMARY, isEnabled = true, size = DashboardWidgetSize.COMPACT),
            DashboardTemplateWidget(DashboardWidgetType.FAVORITES, isEnabled = false, size = DashboardWidgetSize.COMPACT),
        ),
    ),
    QUICK_ACCESS(
        listOf(
            DashboardTemplateWidget(DashboardWidgetType.FAVORITES, isEnabled = true, size = DashboardWidgetSize.DETAILED),
            DashboardTemplateWidget(DashboardWidgetType.STORAGE_SUMMARY, isEnabled = true, size = DashboardWidgetSize.COMPACT),
            DashboardTemplateWidget(DashboardWidgetType.DUPLICATES, isEnabled = false, size = DashboardWidgetSize.COMPACT),
            DashboardTemplateWidget(DashboardWidgetType.INSIGHTS, isEnabled = false, size = DashboardWidgetSize.COMPACT),
        ),
    ),
    AT_A_GLANCE(
        listOf(
            DashboardTemplateWidget(DashboardWidgetType.STORAGE_SUMMARY, isEnabled = true, size = DashboardWidgetSize.DETAILED),
            DashboardTemplateWidget(DashboardWidgetType.DUPLICATES, isEnabled = false, size = DashboardWidgetSize.COMPACT),
            DashboardTemplateWidget(DashboardWidgetType.INSIGHTS, isEnabled = false, size = DashboardWidgetSize.COMPACT),
            DashboardTemplateWidget(DashboardWidgetType.FAVORITES, isEnabled = false, size = DashboardWidgetSize.COMPACT),
        ),
    ),
}
