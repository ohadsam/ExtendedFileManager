package com.efm.filemanager.domain.model

/**
 * Phase 19's widget catalog -- today's four Dashboard cards, as a registry a later phase (or a
 * later Phase 19 slice) can add to, rather than four unconditional composable calls. Declaration
 * order is this slice's display order; there's no user-reorderable sort yet (see docs/PLAN.md
 * Phase 19).
 */
enum class DashboardWidgetType { STORAGE_SUMMARY, DUPLICATES, INSIGHTS, FAVORITES }
