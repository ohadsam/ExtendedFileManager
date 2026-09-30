package com.efm.filemanager.data.security

/**
 * Thrown by [PathGuard.checkNameAllowed] to refuse a user- or archive-supplied name that could
 * escape its intended folder (blank, ".", "..", or containing a path separator) -- see
 * docs/PLAN.md Phase 11.
 */
class PathTraversalException(val attemptedName: String) : Exception("\"$attemptedName\" isn't a valid name")
