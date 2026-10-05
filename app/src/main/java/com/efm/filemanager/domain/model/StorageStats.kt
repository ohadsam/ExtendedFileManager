package com.efm.filemanager.domain.model

private const val DEFAULT_LARGEST_FILES_COUNT = 5

/**
 * Phase 18's storage-usage snapshot across every granted tree. [sizeByCategory] reuses
 * [FileCategory] -- the same classification Browse's own filter chips use -- so "Videos: 4.2 GB"
 * here means exactly the same thing as Browse's "Videos" filter, not a second taxonomy.
 */
data class StorageStats(
    val totalSize: Long = 0L,
    val totalFileCount: Int = 0,
    val sizeByCategory: Map<FileCategory, Long> = emptyMap(),
    val largestFiles: List<FileEntry> = emptyList(),
    val recentlyModifiedFiles: List<FileEntry> = emptyList(),
)

/**
 * Pure aggregation over a flat file list -- the Android-dependent recursive SAF walk that
 * produces this list lives in `StatisticsRepository`, kept separate so this accumulation logic
 * (the part actually worth unit-testing) doesn't need a device/Robolectric to verify. Folders
 * are expected to already be filtered out by the caller, same as every other scan in this app.
 * [largestCount] caps both [largestFiles] and [recentlyModifiedFiles] -- there's no reason for
 * these two "top N" widgets to show a different N.
 */
fun List<FileEntry>.toStorageStats(largestCount: Int = DEFAULT_LARGEST_FILES_COUNT): StorageStats =
    StorageStats(
        totalSize = sumOf { it.size },
        totalFileCount = size,
        sizeByCategory = groupBy { it.category() }.mapValues { (_, files) -> files.sumOf { it.size } },
        largestFiles = sortedByDescending { it.size }.take(largestCount),
        recentlyModifiedFiles = sortedByDescending { it.lastModified }.take(largestCount),
    )
