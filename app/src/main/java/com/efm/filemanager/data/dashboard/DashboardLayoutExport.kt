package com.efm.filemanager.data.dashboard

import com.efm.filemanager.domain.model.DashboardTemplateWidget
import com.efm.filemanager.domain.model.DashboardWidgetSize
import com.efm.filemanager.domain.model.DashboardWidgetType

private const val FIELD_DELIMITER = "|"

/** One saved layout's exportable shape -- the same per-widget [DashboardTemplateWidget] shape a built-in template already uses. */
data class DashboardLayoutExport(
    val name: String,
    val widgets: List<DashboardTemplateWidget>,
)

/**
 * A simple line-based text format, not JSON -- this app has no JSON dependency yet, and a flat
 * list of (type, enabled, size) rows doesn't need one. The first line is the layout's name, every
 * line after is one widget.
 */
internal fun DashboardLayoutExport.serialize(): String {
    val widgetLines = widgets.map { widget -> "${widget.type.name}$FIELD_DELIMITER${widget.isEnabled}$FIELD_DELIMITER${widget.size.name}" }
    return (listOf(name) + widgetLines).joinToString("\n")
}

/** Null on anything malformed -- an export from a future app version, a hand-edited file, or a file that isn't this app's export. */
internal fun parseDashboardLayoutExport(text: String): DashboardLayoutExport? {
    val lines = text.lines().filter { it.isNotBlank() }
    val name = lines.firstOrNull() ?: return null
    val widgets = lines.drop(1).mapNotNull { it.toTemplateWidgetLine() }
    return if (widgets.isEmpty()) null else DashboardLayoutExport(name, widgets)
}

private fun String.toTemplateWidgetLine(): DashboardTemplateWidget? {
    val parts = split(FIELD_DELIMITER)
    if (parts.size != 3) return null
    val type = runCatching { DashboardWidgetType.valueOf(parts[0]) }.getOrNull()
    val isEnabled = parts[1].toBooleanStrictOrNull()
    val size = runCatching { DashboardWidgetSize.valueOf(parts[2]) }.getOrNull()
    return if (type != null && isEnabled != null && size != null) DashboardTemplateWidget(type, isEnabled, size) else null
}
