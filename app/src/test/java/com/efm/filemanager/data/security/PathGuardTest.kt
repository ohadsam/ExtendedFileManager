package com.efm.filemanager.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PathGuardTest {
    @Test
    fun `flags Android data and obb, case-insensitively, at any depth`() {
        assertTrue(isProtectedDocumentPath("primary:Android/data/com.foo/files"))
        assertTrue(isProtectedDocumentPath("primary:android/DATA"))
        assertTrue(isProtectedDocumentPath("primary:Android/obb/com.foo"))
    }

    @Test
    fun `does not flag an ordinary folder, including one that merely starts with the same letters`() {
        assertFalse(isProtectedDocumentPath("primary:Download/photo.jpg"))
        assertFalse(isProtectedDocumentPath("primary:Android"))
        assertFalse(isProtectedDocumentPath("primary:Android/database_backups"))
    }

    @Test
    fun `unsafe names are blank, dot, dot-dot, or contain a path separator`() {
        assertTrue(isUnsafeName(""))
        assertTrue(isUnsafeName("   "))
        assertTrue(isUnsafeName("."))
        assertTrue(isUnsafeName(".."))
        assertTrue(isUnsafeName("a/b"))
        assertTrue(isUnsafeName("a\\b"))
    }

    @Test
    fun `an ordinary name, including one that merely contains dots, is safe`() {
        assertFalse(isUnsafeName("photo.jpg"))
        assertFalse(isUnsafeName("..hidden"))
        assertFalse(isUnsafeName("report.v2.tmp"))
        assertEquals(false, isUnsafeName("my folder"))
    }
}
