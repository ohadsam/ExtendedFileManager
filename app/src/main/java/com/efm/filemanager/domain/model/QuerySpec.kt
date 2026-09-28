package com.efm.filemanager.domain.model

enum class SortField { NAME, SIZE, DATE_MODIFIED }

enum class SortOrder { ASCENDING, DESCENDING }

enum class GroupBy { NONE, TYPE, SOURCE_APP, DATE_MODIFIED }

enum class FileTypeFilter { ALL, FILES_ONLY, FOLDERS_ONLY }

enum class SizePreset { ANY, UNDER_1MB, MB_1_TO_10, MB_10_TO_100, OVER_100MB }

enum class DatePreset { ANY, TODAY, LAST_7_DAYS, LAST_30_DAYS, OLDER_THAN_30_DAYS }

/**
 * One reusable filter/sort/group/search spec, applied the same way from browse and from
 * global search (search is just this spec with [freeText] filled in) -- see docs/PLAN.md
 * Phase 5. [tag]/[favoriteOnly]/[lockedOnly] are reserved dimensions: the data behind them
 * doesn't exist until Phase 9, so filtering on them is a no-op today; they're defined here
 * now so that phase only has to wire a UI, not extend this spec.
 */
data class QuerySpec(
    val freeText: String = "",
    val typeFilter: FileTypeFilter = FileTypeFilter.ALL,
    val category: FileCategory? = null,
    val sourceAppPackage: String? = null,
    val sizePreset: SizePreset = SizePreset.ANY,
    val datePreset: DatePreset = DatePreset.ANY,
    val tag: String? = null,
    val favoriteOnly: Boolean = false,
    val lockedOnly: Boolean = false,
    val sortField: SortField = SortField.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val groupBy: GroupBy = GroupBy.NONE,
) {
    fun clearFilters(): QuerySpec =
        copy(
            typeFilter = FileTypeFilter.ALL,
            category = null,
            sourceAppPackage = null,
            sizePreset = SizePreset.ANY,
            datePreset = DatePreset.ANY,
        )
}
