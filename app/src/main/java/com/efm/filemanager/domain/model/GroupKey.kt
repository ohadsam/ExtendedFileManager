package com.efm.filemanager.domain.model

enum class DateBucket { TODAY, THIS_WEEK, THIS_MONTH, OLDER }

sealed interface GroupKey {
    data object None : GroupKey

    data class Category(val category: FileCategory) : GroupKey

    data class SourceApp(val packageName: String?) : GroupKey

    data class ModifiedDate(val bucket: DateBucket) : GroupKey

    data class Tag(val tagId: Long, val tagName: String) : GroupKey

    data object NoTags : GroupKey
}
