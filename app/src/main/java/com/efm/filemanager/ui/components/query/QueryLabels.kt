package com.efm.filemanager.ui.components.query

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R
import com.efm.filemanager.domain.model.DateBucket
import com.efm.filemanager.domain.model.DatePreset
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.FileTypeFilter
import com.efm.filemanager.domain.model.GroupKey
import com.efm.filemanager.domain.model.SizePreset

internal fun FileTypeFilter.labelRes(): Int =
    when (this) {
        FileTypeFilter.ALL -> R.string.filter_show_all
        FileTypeFilter.FILES_ONLY -> R.string.filter_show_files
        FileTypeFilter.FOLDERS_ONLY -> R.string.filter_show_folders
    }

internal fun FileCategory.labelRes(): Int =
    when (this) {
        FileCategory.IMAGE -> R.string.category_image
        FileCategory.VIDEO -> R.string.category_video
        FileCategory.AUDIO -> R.string.category_audio
        FileCategory.DOCUMENT -> R.string.category_document
        FileCategory.ARCHIVE -> R.string.category_archive
        FileCategory.APK -> R.string.category_apk
        FileCategory.FOLDER -> R.string.category_folder
        FileCategory.OTHER -> R.string.category_other
    }

internal fun SizePreset.labelRes(): Int =
    when (this) {
        SizePreset.ANY -> R.string.size_any
        SizePreset.UNDER_1MB -> R.string.size_under_1mb
        SizePreset.MB_1_TO_10 -> R.string.size_1_10mb
        SizePreset.MB_10_TO_100 -> R.string.size_10_100mb
        SizePreset.OVER_100MB -> R.string.size_over_100mb
    }

internal fun DatePreset.labelRes(): Int =
    when (this) {
        DatePreset.ANY -> R.string.date_any
        DatePreset.TODAY -> R.string.date_today
        DatePreset.LAST_7_DAYS -> R.string.date_last_7_days
        DatePreset.LAST_30_DAYS -> R.string.date_last_30_days
        DatePreset.OLDER_THAN_30_DAYS -> R.string.date_older_30_days
    }

internal fun DateBucket.labelRes(): Int =
    when (this) {
        DateBucket.TODAY -> R.string.date_bucket_today
        DateBucket.THIS_WEEK -> R.string.date_bucket_this_week
        DateBucket.THIS_MONTH -> R.string.date_bucket_this_month
        DateBucket.OLDER -> R.string.date_bucket_older
    }

/**
 * A group header's display text. Source-app groups show the raw package name for now --
 * resolving it to a human label + icon via PackageManager (Phase 1's plan for it) is a
 * display-polish concern better suited to Phase 8's UI pass, not this filter/sort/group engine.
 */
@Composable
internal fun GroupKey.displayLabel(): String =
    when (this) {
        GroupKey.None -> ""
        is GroupKey.Category -> stringResource(category.labelRes())
        is GroupKey.SourceApp -> packageName ?: stringResource(R.string.source_app_unknown)
        is GroupKey.ModifiedDate -> stringResource(bucket.labelRes())
        is GroupKey.Tag -> tagName
        GroupKey.NoTags -> stringResource(R.string.group_no_tags)
    }
