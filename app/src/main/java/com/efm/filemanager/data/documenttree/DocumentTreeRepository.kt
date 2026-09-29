package com.efm.filemanager.data.documenttree

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.efm.filemanager.data.local.FileEntryDao
import com.efm.filemanager.data.local.FileEntryEntity
import com.efm.filemanager.data.local.toDomain
import com.efm.filemanager.data.metadata.FileMetadataRepository
import com.efm.filemanager.data.metadata.enrich
import com.efm.filemanager.data.sourceapp.SourceAppResolver
import com.efm.filemanager.domain.model.FileEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Lists folder contents via Storage Access Framework and caches the result in Room so
 * the UI reads from a reactive local source instead of re-querying the SAF provider on
 * every recomposition. [refresh] re-lists from SAF and replaces the cached children;
 * [observeChildren] is what the UI actually collects.
 */
class DocumentTreeRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val fileEntryDao: FileEntryDao,
        private val sourceAppResolver: SourceAppResolver,
        private val fileMetadataRepository: FileMetadataRepository,
    ) {
        fun observeChildren(parentUri: Uri): Flow<List<FileEntry>> =
            combine(fileEntryDao.observeChildren(parentUri.toString()), fileMetadataRepository.snapshot) { entries, snapshot ->
                entries.map { it.toDomain().enrich(snapshot) }
            }

        suspend fun refresh(parentUri: Uri) {
            val entries = listChildrenFromSaf(parentUri)
            fileEntryDao.replaceChildren(parentUri.toString(), entries)
        }

        /** Resolves a file/folder's parent from the cached index, if it's been listed before. */
        suspend fun parentUriOf(uri: Uri): Uri? =
            withContext(Dispatchers.IO) { fileEntryDao.getByUri(uri.toString())?.parentUri?.let(Uri::parse) }

        /**
         * Walks [folderUri]'s ancestors up through the cached index (each folder's own row was
         * written when its parent was listed) until it reaches a uri no row exists for -- that's
         * necessarily a granted tree root, since only children ever get indexed, never the root
         * itself. Used to rebuild a breadcrumb trail for a location found via search rather than
         * by browsing down to it.
         */
        suspend fun ancestorChain(folderUri: Uri): LocationChain =
            withContext(Dispatchers.IO) {
                val chain = mutableListOf<FileEntry>()
                var currentUri = folderUri.toString()
                var currentEntity = fileEntryDao.getByUri(currentUri)
                while (currentEntity != null) {
                    chain.add(0, currentEntity.toDomain())
                    currentUri = currentEntity.parentUri
                    currentEntity = fileEntryDao.getByUri(currentUri)
                }
                LocationChain(rootUri = Uri.parse(currentUri), folders = chain)
            }

        // internal, not private: reused by SearchIndexRepository to walk the whole granted
        // tree for its recursive search index, instead of duplicating this SAF-listing logic.
        internal suspend fun listChildrenFromSaf(parentUri: Uri): List<FileEntryEntity> =
            withContext(Dispatchers.IO) {
                // fromTreeUri (not fromSingleUri) is required here: it's the only factory that
                // returns a TreeDocumentFile, whose listFiles() actually works for hierarchical
                // navigation. It transparently accepts both the bare tree-root URI and a full
                // tree-scoped document URI for a nested folder, so this one call covers every
                // navigation depth. fromSingleUri's SingleDocumentFile.listFiles() throws
                // UnsupportedOperationException -- easy mistake since both factories compile fine.
                val parentDocument = DocumentFile.fromTreeUri(context, parentUri) ?: return@withContext emptyList()
                val parentFolderName = parentDocument.name
                parentDocument.listFiles()
                    .filterNot { it.name == TRASH_FOLDER_NAME }
                    .mapNotNull { child -> child.toEntity(parentUri, parentFolderName) }
            }

        private fun DocumentFile.toEntity(
            parentUri: Uri,
            parentFolderName: String?,
        ): FileEntryEntity? {
            val childUri = uri
            val childName = name ?: return null
            val childSize = length()
            val sourceApp =
                if (isDirectory) {
                    null
                } else {
                    sourceAppResolver.resolve(childName, childSize, parentFolderName)
                }
            return FileEntryEntity(
                uri = childUri.toString(),
                parentUri = parentUri.toString(),
                documentId = runCatching { DocumentsContract.getDocumentId(childUri) }.getOrDefault(childUri.toString()),
                name = childName,
                isDirectory = isDirectory,
                size = childSize,
                lastModified = lastModified(),
                mimeType = type,
                ownerPackageName = sourceApp?.packageName,
                sourceConfidence = sourceApp?.confidence?.name,
            )
        }
    }
