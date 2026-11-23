package com.efm.filemanager.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckRepositoryTest {
    @Test
    fun `a higher remote patch version is newer`() {
        assertTrue(isNewerVersion(remoteVersion = "1.2.4", currentVersion = "1.2.3"))
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(isNewerVersion(remoteVersion = "1.2.3", currentVersion = "1.2.3"))
    }

    @Test
    fun `an older remote version is not newer`() {
        assertFalse(isNewerVersion(remoteVersion = "1.2.2", currentVersion = "1.2.3"))
    }

    @Test
    fun `a leading v tag is stripped before comparing`() {
        assertTrue(isNewerVersion(remoteVersion = "v1.3.0", currentVersion = "1.2.9"))
    }

    @Test
    fun `a missing trailing component is treated as zero`() {
        assertFalse(isNewerVersion(remoteVersion = "1.2", currentVersion = "1.2.0"))
        assertTrue(isNewerVersion(remoteVersion = "1.3", currentVersion = "1.2.9"))
    }

    @Test
    fun `a higher major version wins even with a lower minor version`() {
        assertTrue(isNewerVersion(remoteVersion = "2.0.0", currentVersion = "1.9.9"))
    }

    @Test
    fun `the first asset ending in apk is picked`() {
        val assets = listOf("README.md" to "readme-url", "app-release.apk" to "apk-url", "notes.txt" to "notes-url")

        assertEquals("app-release.apk" to "apk-url", findApkAsset(assets))
    }

    @Test
    fun `no apk asset yields null`() {
        assertNull(findApkAsset(listOf("README.md" to "readme-url")))
    }

    @Test
    fun `an empty asset list yields null`() {
        assertNull(findApkAsset(emptyList()))
    }
}
