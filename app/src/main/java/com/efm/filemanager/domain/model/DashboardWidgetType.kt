package com.efm.filemanager.domain.model

/**
 * Phase 19's widget catalog -- today's Dashboard cards, as a registry a later phase (or a later
 * Phase 19 slice) can add to, rather than a fixed set of unconditional composable calls.
 * [LOW_STORAGE] is the one automatic entry: enabled by default like any other, but its card only
 * actually renders once free space is genuinely low (see [com.efm.filemanager.data.statistics.isLowStorage]),
 * never a widget the user has to notice and turn on themselves.
 */
enum class DashboardWidgetType { STORAGE_SUMMARY, DUPLICATES, INSIGHTS, FAVORITES, LOW_STORAGE }
