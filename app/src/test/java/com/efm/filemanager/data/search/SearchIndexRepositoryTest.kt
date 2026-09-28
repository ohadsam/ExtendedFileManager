package com.efm.filemanager.data.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchIndexRepositoryTest {
    @Test
    fun `single word becomes a prefix query`() {
        assertEquals("invoice*", buildFtsQuery("invoice"))
    }

    @Test
    fun `multiple words each become their own prefix query`() {
        assertEquals("summer* 2024*", buildFtsQuery("summer 2024"))
    }

    @Test
    fun `punctuation splits into separate tokens instead of breaking FTS syntax`() {
        assertEquals("invoice* pdf*", buildFtsQuery("invoice.pdf"))
    }

    @Test
    fun `blank input yields a blank query`() {
        assertEquals("", buildFtsQuery("   "))
    }

    @Test
    fun `non-latin text such as Hebrew is preserved as a token`() {
        assertEquals("תמונה*", buildFtsQuery("תמונה"))
    }
}
