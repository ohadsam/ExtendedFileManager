package com.efm.filemanager.domain.query

import android.net.Uri
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.FileTag
import com.efm.filemanager.domain.model.FileTypeFilter
import com.efm.filemanager.domain.model.GroupBy
import com.efm.filemanager.domain.model.GroupKey
import com.efm.filemanager.domain.model.QuerySpec
import com.efm.filemanager.domain.model.SortField
import com.efm.filemanager.domain.model.SortOrder
import com.efm.filemanager.domain.model.TagColor
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FileQueryEngineTest {
    @Test
    fun `folders sort before files regardless of the chosen field`() {
        val files = listOf(entry("b.txt"), entry("folder", flags = setOf(EntryFlag.DIRECTORY)), entry("a.txt"))

        val result = QuerySpec().applyTo(files)

        assertEquals(listOf("folder", "a.txt", "b.txt"), result.map { it.name })
    }

    @Test
    fun `descending order reverses the field but not the folders-first rule`() {
        val files = listOf(entry("a.txt"), entry("folder", flags = setOf(EntryFlag.DIRECTORY)), entry("b.txt"))
        val spec = QuerySpec(sortField = SortField.NAME, sortOrder = SortOrder.DESCENDING)

        val result = spec.applyTo(files)

        assertEquals(listOf("folder", "b.txt", "a.txt"), result.map { it.name })
    }

    @Test
    fun `sort by size orders smallest to largest among files`() {
        val files = listOf(entry("big", size = 300), entry("small", size = 100), entry("mid", size = 200))
        val spec = QuerySpec(sortField = SortField.SIZE)

        val result = spec.applyTo(files)

        assertEquals(listOf("small", "mid", "big"), result.map { it.name })
    }

    @Test
    fun `files-only filter drops directories`() {
        val files = listOf(entry("a.txt"), entry("folder", flags = setOf(EntryFlag.DIRECTORY)))
        val spec = QuerySpec(typeFilter = FileTypeFilter.FILES_ONLY)

        val result = spec.applyTo(files)

        assertEquals(listOf("a.txt"), result.map { it.name })
    }

    @Test
    fun `free text filter matches names case-insensitively`() {
        val files = listOf(entry("Invoice.pdf"), entry("photo.jpg"))
        val spec = QuerySpec(freeText = "invoice")

        val result = spec.applyTo(files)

        assertEquals(listOf("Invoice.pdf"), result.map { it.name })
    }

    @Test
    fun `no grouping returns a single ungrouped bucket`() {
        val files = listOf(entry("a.txt"), entry("b.jpg", mimeType = "image/jpeg"))

        val groups = QuerySpec().groupResult(files)

        assertEquals(1, groups.size)
        assertEquals(GroupKey.None, groups.single().key)
        assertEquals(2, groups.single().files.size)
    }

    @Test
    fun `group by type buckets images separately from other files`() {
        val files = listOf(entry("a.txt"), entry("b.jpg", mimeType = "image/jpeg"), entry("c.jpg", mimeType = "image/jpeg"))
        val spec = QuerySpec(groupBy = GroupBy.TYPE)

        val groups = spec.groupResult(files)

        val imageGroup = groups.first { it.key is GroupKey.Category && (it.key as GroupKey.Category).category.name == "IMAGE" }
        assertEquals(2, imageGroup.files.size)
    }

    @Test
    fun `tag filter keeps files carrying any of the selected tags`() {
        val important = FileTag(id = 1, name = "Important", color = TagColor.RED, pinned = false)
        val work = FileTag(id = 2, name = "Work", color = TagColor.BLUE, pinned = false)
        val files = listOf(entry("a.txt", tags = listOf(important)), entry("b.txt", tags = listOf(work)), entry("c.txt"))
        val spec = QuerySpec(tagIds = setOf(1L))

        val result = spec.applyTo(files)

        assertEquals(listOf("a.txt"), result.map { it.name })
    }

    @Test
    fun `favorite-only filter keeps only favorited entries`() {
        val files = listOf(entry("a.txt", flags = setOf(EntryFlag.FAVORITE)), entry("b.txt"))
        val spec = QuerySpec(favoriteOnly = true)

        val result = spec.applyTo(files)

        assertEquals(listOf("a.txt"), result.map { it.name })
    }

    @Test
    fun `locked-only filter keeps only locked entries`() {
        val files = listOf(entry("a.txt", flags = setOf(EntryFlag.LOCKED)), entry("b.txt"))
        val spec = QuerySpec(lockedOnly = true)

        val result = spec.applyTo(files)

        assertEquals(listOf("a.txt"), result.map { it.name })
    }

    @Test
    fun `group by tag puts a multi-tagged file in every one of its tags' groups`() {
        val important = FileTag(id = 1, name = "Important", color = TagColor.RED, pinned = false)
        val work = FileTag(id = 2, name = "Work", color = TagColor.BLUE, pinned = false)
        val files = listOf(entry("a.txt", tags = listOf(important, work)), entry("b.txt", tags = listOf(work)), entry("c.txt"))
        val spec = QuerySpec(groupBy = GroupBy.TAG)

        val groups = spec.groupResult(files)

        val importantGroup = groups.first { it.key == GroupKey.Tag(1, "Important") }
        val workGroup = groups.first { it.key == GroupKey.Tag(2, "Work") }
        val noTagsGroup = groups.first { it.key == GroupKey.NoTags }
        assertEquals(listOf("a.txt"), importantGroup.files.map { it.name })
        assertEquals(listOf("a.txt", "b.txt"), workGroup.files.map { it.name })
        assertEquals(listOf("c.txt"), noTagsGroup.files.map { it.name })
    }

    private enum class EntryFlag { DIRECTORY, FAVORITE, LOCKED }

    private fun entry(
        name: String,
        size: Long = 0L,
        mimeType: String? = null,
        tags: List<FileTag> = emptyList(),
        flags: Set<EntryFlag> = emptySet(),
    ) = FileEntry(
        uri = mockk<Uri>(),
        documentId = name,
        name = name,
        isDirectory = EntryFlag.DIRECTORY in flags,
        size = size,
        lastModified = System.currentTimeMillis(),
        mimeType = mimeType,
        sourceApp = null,
        tags = tags,
        isFavorite = EntryFlag.FAVORITE in flags,
        isLocked = EntryFlag.LOCKED in flags,
    )
}
