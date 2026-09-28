package com.efm.filemanager.data.archive

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.efm.filemanager.data.documenttree.DocumentTreeRepository
import com.efm.filemanager.data.documenttree.requireSingleDocument
import com.efm.filemanager.data.documenttree.requireTreeDocument
import com.efm.filemanager.data.documenttree.uniqueNameIn
import com.efm.filemanager.domain.model.FileEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

private const val ZIP_MIME_TYPE = "application/zip"
private const val DEFAULT_MIME_TYPE = "application/octet-stream"

/**
 * Zip create/extract, streamed directly against SAF `content://` URIs (no temp-file copy).
 * Progress is reported as a running count rather than a percentage -- getting a true total
 * ahead of time would mean reading the whole archive twice, which isn't worth it for the
 * "how many files so far" feedback a progress dialog actually needs.
 */
class ArchiveRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val documentTreeRepository: DocumentTreeRepository,
    ) {
        suspend fun compress(
            entries: List<FileEntry>,
            targetParentUri: Uri,
            archiveName: String,
            onProgress: (Int) -> Unit,
        ): Result<Unit> =
            runCatching {
                withContext(Dispatchers.IO) {
                    val targetParent = requireTreeDocument(context, targetParentUri)
                    val archiveFile =
                        requireNotNull(targetParent.createFile(ZIP_MIME_TYPE, archiveName)) { "Could not create archive" }
                    val sources = collectCompressSources(entries)
                    writeZip(archiveFile.uri, sources, onProgress)
                    documentTreeRepository.refresh(targetParentUri)
                }
            }

        suspend fun extract(
            archive: FileEntry,
            targetParentUri: Uri,
            conflictPolicy: ConflictPolicy,
            onProgress: (Int) -> Unit,
        ): Result<Unit> =
            runCatching {
                withContext(Dispatchers.IO) {
                    val targetParent = requireTreeDocument(context, targetParentUri)
                    readZip(archive.uri, targetParent, conflictPolicy, onProgress)
                    documentTreeRepository.refresh(targetParentUri)
                }
            }

        private fun collectCompressSources(entries: List<FileEntry>): List<Pair<String, DocumentFile>> {
            val sources = mutableListOf<Pair<String, DocumentFile>>()
            entries.forEach { entry ->
                if (entry.isDirectory) {
                    collectRecursively(requireTreeDocument(context, entry.uri), entry.name, sources)
                } else {
                    sources.add(entry.name to requireSingleDocument(context, entry.uri))
                }
            }
            return sources
        }

        private fun collectRecursively(
            dir: DocumentFile,
            path: String,
            out: MutableList<Pair<String, DocumentFile>>,
        ) {
            dir.listFiles().forEach { child ->
                val childName = child.name
                if (childName != null) {
                    val childPath = "$path/$childName"
                    if (child.isDirectory) {
                        collectRecursively(child, childPath, out)
                    } else {
                        out.add(childPath to child)
                    }
                }
            }
        }

        private fun writeZip(
            archiveUri: Uri,
            sources: List<Pair<String, DocumentFile>>,
            onProgress: (Int) -> Unit,
        ) {
            context.contentResolver.openOutputStream(archiveUri)?.use { rawOut ->
                ZipOutputStream(rawOut).use { zipOut ->
                    sources.forEachIndexed { index, (path, file) ->
                        zipOut.putNextEntry(ZipEntry(path))
                        context.contentResolver.openInputStream(file.uri)?.use { input -> input.copyTo(zipOut) }
                        zipOut.closeEntry()
                        onProgress(index + 1)
                    }
                }
            }
        }

        private fun readZip(
            archiveUri: Uri,
            targetParent: DocumentFile,
            conflictPolicy: ConflictPolicy,
            onProgress: (Int) -> Unit,
        ) {
            val dirCache = mutableMapOf<String, DocumentFile>()
            var extractedCount = 0
            context.contentResolver.openInputStream(archiveUri)?.use { rawIn ->
                ZipInputStream(rawIn).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val (parent, fileName) = resolveEntryParent(targetParent, entry.name, dirCache)
                            if (writeEntry(parent, fileName, zipIn, conflictPolicy)) extractedCount++
                            onProgress(extractedCount)
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
        }

        private fun resolveEntryParent(
            root: DocumentFile,
            entryName: String,
            dirCache: MutableMap<String, DocumentFile>,
        ): Pair<DocumentFile, String> {
            val segments = entryName.split('/').filter { it.isNotEmpty() }
            var currentDir = root
            var pathSoFar = ""
            segments.dropLast(1).forEach { segment ->
                pathSoFar = if (pathSoFar.isEmpty()) segment else "$pathSoFar/$segment"
                currentDir =
                    dirCache.getOrPut(pathSoFar) {
                        val dir = currentDir
                        requireNotNull(dir.findFile(segment) ?: dir.createDirectory(segment)) {
                            "Could not create folder $segment"
                        }
                    }
            }
            return currentDir to segments.last()
        }

        private fun writeEntry(
            parent: DocumentFile,
            desiredName: String,
            input: InputStream,
            policy: ConflictPolicy,
        ): Boolean {
            val existing = parent.findFile(desiredName)
            if (existing != null && policy == ConflictPolicy.SKIP) return false
            if (existing != null && policy == ConflictPolicy.OVERWRITE) existing.delete()
            val name = if (existing != null && policy == ConflictPolicy.RENAME) uniqueNameIn(parent, desiredName) else desiredName
            val newFile = parent.createFile(guessMimeType(desiredName), name)
            return if (newFile == null) {
                false
            } else {
                context.contentResolver.openOutputStream(newFile.uri)?.use { output -> input.copyTo(output) }
                true
            }
        }

        private fun guessMimeType(fileName: String): String {
            val extension = fileName.substringAfterLast('.', "")
            if (extension.isEmpty()) return DEFAULT_MIME_TYPE
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: DEFAULT_MIME_TYPE
        }
    }
