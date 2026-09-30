package com.efm.filemanager.data.security

import android.net.Uri
import android.provider.DocumentsContract

/**
 * True if [documentPath] (an SAF document id, e.g. "primary:Android/data/com.foo/files") sits
 * inside a path EFM refuses to mutate directly. Pure and unit-tested independent of any Android
 * API -- see [PathGuard] for the URI-decoding glue around this.
 */
internal fun isProtectedDocumentPath(documentPath: String): Boolean {
    val segments = documentPath.substringAfter(':').lowercase().split('/').filter { it.isNotEmpty() }
    return segments.size >= 2 && segments[0] == "android" && (segments[1] == "data" || segments[1] == "obb")
}

/**
 * True if [name] is unsafe to use as a created/renamed/extracted display name -- blank, ".",
 * "..", or containing a path separator. Pure and unit-tested.
 */
internal fun isUnsafeName(name: String): Boolean =
    name.isBlank() || name == "." || name == ".." || name.contains('/') || name.contains('\\')

/**
 * Guards every mutating [com.efm.filemanager.data.documenttree.FileOperationsRepository] and
 * [com.efm.filemanager.data.archive.ArchiveRepository] call -- see docs/PLAN.md Phase 11. Each
 * check throws rather than returning a boolean so a single `PathGuard.checkAllowed(...)` call
 * inline in an existing `runCatching`/`runOperation` block is enough; the surrounding block's
 * own failure handling (and audit logging) picks the exception up same as any other failure.
 */
object PathGuard {
    /** Rejects a mutation whose target document sits inside a protected path. */
    fun checkAllowed(uri: Uri) {
        val documentPath = documentPathOf(uri) ?: return
        if (isProtectedDocumentPath(documentPath)) throw ProtectedPathException(documentPath)
    }

    /** Rejects a user- or archive-supplied display name that could traverse out of its folder. */
    fun checkNameAllowed(name: String) {
        if (isUnsafeName(name)) throw PathTraversalException(name)
    }

    // getDocumentId needs a URI with a /document/ path segment (true for every nested SAF
    // child, and for fromSingleUri results); a bare granted-root tree URI doesn't have one, so
    // getTreeDocumentId -- which reads the tree segment directly -- is the fallback for that case.
    private fun documentPathOf(uri: Uri): String? =
        runCatching { DocumentsContract.getDocumentId(uri) }
            .recoverCatching { DocumentsContract.getTreeDocumentId(uri) }
            .getOrNull()
}
