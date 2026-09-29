package com.efm.filemanager.domain.query

import com.efm.filemanager.domain.model.DateBucket
import com.efm.filemanager.domain.model.DatePreset
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileGroup
import com.efm.filemanager.domain.model.FileTypeFilter
import com.efm.filemanager.domain.model.GroupBy
import com.efm.filemanager.domain.model.GroupKey
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.SizePreset
import com.efm.filemanager.domain.model.SortField
import com.efm.filemanager.domain.model.SortOrder
import com.efm.filemanager.domain.model.category
import java.util.concurrent.TimeUnit

private const val ONE_MB = 1024L * 1024L

/** Filters and sorts [files] per [spec] -- the one path both Browse and Search render through. */
fun QuerySpec.applyTo(files: List<FileEntry>): List<FileEntry> = files.filterBySpec(this).sortedBySpec(this)

/** [applyTo], then buckets the result per [QuerySpec.groupBy]; a single ungrouped bucket when it's [GroupBy.NONE]. */
fun QuerySpec.groupResult(files: List<FileEntry>): List<FileGroup> {
    val filtered = applyTo(files)
    if (groupBy == GroupBy.NONE) return listOf(FileGroup(key = GroupKey.None, files = filtered))
    return filtered.groupBy { entry -> groupKeyFor(entry, groupBy) }.map { (key, entries) -> FileGroup(key = key, files = entries) }
}

private fun List<FileEntry>.filterBySpec(spec: QuerySpec): List<FileEntry> =
    filter { entry -> matchesType(entry, spec.typeFilter) }
        .filter { entry -> spec.category == null || entry.category() == spec.category }
        .filter { entry -> spec.sourceAppPackage == null || entry.sourceApp?.packageName == spec.sourceAppPackage }
        .filter { entry -> matchesSize(entry, spec.sizePreset) }
        .filter { entry -> matchesDate(entry, spec.datePreset) }
        .filter { entry -> spec.freeText.isBlank() || entry.name.contains(spec.freeText, ignoreCase = true) }
        .filter { entry -> spec.tagIds.isEmpty() || entry.tags.any { tag -> tag.id in spec.tagIds } }
        .filter { entry -> !spec.favoriteOnly || entry.isFavorite }
        .filter { entry -> !spec.lockedOnly || entry.isLocked }

private fun matchesType(
    entry: FileEntry,
    filter: FileTypeFilter,
): Boolean =
    when (filter) {
        FileTypeFilter.ALL -> true
        FileTypeFilter.FILES_ONLY -> !entry.isDirectory
        FileTypeFilter.FOLDERS_ONLY -> entry.isDirectory
    }

private fun matchesSize(
    entry: FileEntry,
    preset: SizePreset,
): Boolean {
    if (entry.isDirectory || preset == SizePreset.ANY) return true
    val megabytes = entry.size / ONE_MB
    return when (preset) {
        SizePreset.ANY -> true
        SizePreset.UNDER_1MB -> entry.size < ONE_MB
        SizePreset.MB_1_TO_10 -> megabytes in 1..9
        SizePreset.MB_10_TO_100 -> megabytes in 10..99
        SizePreset.OVER_100MB -> megabytes >= 100
    }
}

private fun matchesDate(
    entry: FileEntry,
    preset: DatePreset,
): Boolean {
    if (preset == DatePreset.ANY) return true
    val ageMillis = System.currentTimeMillis() - entry.lastModified
    val day = TimeUnit.DAYS.toMillis(1)
    return when (preset) {
        DatePreset.ANY -> true
        DatePreset.TODAY -> ageMillis < day
        DatePreset.LAST_7_DAYS -> ageMillis < day * 7
        DatePreset.LAST_30_DAYS -> ageMillis < day * 30
        DatePreset.OLDER_THAN_30_DAYS -> ageMillis >= day * 30
    }
}

private fun List<FileEntry>.sortedBySpec(spec: QuerySpec): List<FileEntry> {
    val comparator = fieldComparator(spec.sortField)
    val ordered = if (spec.sortOrder == SortOrder.DESCENDING) comparator.reversed() else comparator
    // Folders always sort first regardless of the chosen field/order -- same convention as
    // the plain browse listing (FileEntryDao's own ORDER BY isDirectory DESC).
    return sortedWith(compareByDescending<FileEntry> { it.isDirectory }.then(ordered))
}

private fun fieldComparator(field: SortField): Comparator<FileEntry> =
    when (field) {
        SortField.NAME -> Comparator { a, b -> a.name.compareTo(b.name, ignoreCase = true) }
        SortField.SIZE -> compareBy { it.size }
        SortField.DATE_MODIFIED -> compareBy { it.lastModified }
    }

private fun groupKeyFor(
    entry: FileEntry,
    groupBy: GroupBy,
): GroupKey =
    when (groupBy) {
        GroupBy.NONE -> GroupKey.None
        GroupBy.TYPE -> GroupKey.Category(entry.category())
        GroupBy.SOURCE_APP -> GroupKey.SourceApp(entry.sourceApp?.packageName)
        GroupBy.DATE_MODIFIED -> GroupKey.ModifiedDate(dateBucketFor(entry.lastModified))
    }

private fun dateBucketFor(epochMillis: Long): DateBucket {
    val ageMillis = System.currentTimeMillis() - epochMillis
    val day = TimeUnit.DAYS.toMillis(1)
    return when {
        ageMillis < day -> DateBucket.TODAY
        ageMillis < day * 7 -> DateBucket.THIS_WEEK
        ageMillis < day * 30 -> DateBucket.THIS_MONTH
        else -> DateBucket.OLDER
    }
}
