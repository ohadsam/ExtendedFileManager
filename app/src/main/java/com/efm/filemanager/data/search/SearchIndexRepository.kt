package com.efm.filemanager.data.search

import android.net.Uri
import com.efm.filemanager.data.documenttree.DocumentTreeAccessManager
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.local.FileEntryDao
import com.efm.filemanager.data.local.FileSearchDao
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.data.local.toFtsEntity
import com.efm.filemanager.domain.model.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Recursively indexes every granted tree into the FTS mirror table so global search stays a
 * fast local query rather than re-walking storage on every keystroke -- see docs/PLAN.md
 * Phase 5. [rebuildIndex] rebuilds from scratch: simpler and safer than trying to patch a
 * stale FTS index in place, and cheap enough at this app's scale (single-user local storage,
 * not a server-side index). A smarter incremental rebuild, keyed off a change-version like
 * Phase 17's insights cache, is a reasonable later refinement once this index is also relied
 * on for something latency-sensitive.
 */
class SearchIndexRepository
    @Inject
    constructor(
        private val documentTreeAccessManager: DocumentTreeAccessManager,
        private val documentTreeRepository: DocumentTreeRepository,
        private val fileEntryDao: FileEntryDao,
        private val fileSearchDao: FileSearchDao,
    ) {
        suspend fun rebuildIndex(): Int =
            withContext(Dispatchers.IO) {
                fileSearchDao.clear()
                documentTreeAccessManager.grantedTreeUris().sumOf { root -> indexFolder(root) }
            }

        suspend fun search(rawQuery: String): List<FileEntry> {
            val ftsQuery = buildFtsQuery(rawQuery)
            if (ftsQuery.isBlank()) return emptyList()
            val uris = fileSearchDao.matchUris(ftsQuery)
            return if (uris.isEmpty()) emptyList() else fileEntryDao.getByUris(uris).map { it.toDomain() }
        }

        private suspend fun indexFolder(folderUri: Uri): Int {
            val entries = documentTreeRepository.listChildrenFromSaf(folderUri)
            fileEntryDao.replaceChildren(folderUri.toString(), entries)
            fileSearchDao.insertAll(entries.map { it.toFtsEntity() })
            val childFolderCount = entries.filter { it.isDirectory }.sumOf { indexFolder(Uri.parse(it.uri)) }
            return entries.size + childFolderCount
        }
    }

/**
 * Turns free text into an FTS4 prefix query (`word1* word2*`). Splits on anything that isn't a
 * letter/digit -- not just whitespace -- so "invoice.pdf" becomes two matchable tokens instead
 * of one word merged with its extension, and so nothing punctuation-shaped can break FTS syntax.
 */
internal fun buildFtsQuery(rawQuery: String): String =
    rawQuery
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { "$it*" }
