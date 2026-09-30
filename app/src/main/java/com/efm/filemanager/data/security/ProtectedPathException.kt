package com.efm.filemanager.data.security

/**
 * Thrown by [PathGuard.checkAllowed] to refuse a mutation targeting Android/data, Android/obb,
 * or another app's private storage -- see docs/PLAN.md Phase 11. Scoped storage already makes
 * most of these inherently unreachable, but a granted root can still expose the ones that
 * aren't, so this is an explicit guard rather than relying on the SAF provider to fail (or not)
 * on its own.
 */
class ProtectedPathException(val path: String) : Exception("This is a protected system location and can't be modified: $path")
