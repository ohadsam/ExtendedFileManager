package com.efm.filemanager.domain.model

/**
 * Phase 19's per-widget size. COMPACT trims a card down to its headline number/line; DETAILED
 * (the seeded default) is every widget's original, pre-resizing content.
 */
enum class DashboardWidgetSize { COMPACT, DETAILED }

/** Pure flip between the two -- the edit-mode size button has nothing more to decide than this. */
fun DashboardWidgetSize.toggled(): DashboardWidgetSize =
    if (this == DashboardWidgetSize.DETAILED) DashboardWidgetSize.COMPACT else DashboardWidgetSize.DETAILED
