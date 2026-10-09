package com.efm.filemanager.data.metadata

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY_MILLIS = 1000L * 60 * 60 * 24
private const val NOW = 1_700_000_000_000L

class FileFlagsRepositoryTest {
    @Test
    fun `not staged is never overdue`() {
        assertFalse(isOverdueForReview(stagedAt = null, reviewWindowDays = 30, now = NOW))
    }

    @Test
    fun `staged well within the review window is not overdue`() {
        val stagedAt = NOW - DAY_MILLIS * 5
        assertFalse(isOverdueForReview(stagedAt, reviewWindowDays = 30, now = NOW))
    }

    @Test
    fun `staged exactly at the review window is overdue`() {
        val stagedAt = NOW - DAY_MILLIS * 30
        assertTrue(isOverdueForReview(stagedAt, reviewWindowDays = 30, now = NOW))
    }

    @Test
    fun `staged past the review window is overdue`() {
        val stagedAt = NOW - DAY_MILLIS * 45
        assertTrue(isOverdueForReview(stagedAt, reviewWindowDays = 30, now = NOW))
    }

    @Test
    fun `a shorter configured window flags something a longer one would not`() {
        val stagedAt = NOW - DAY_MILLIS * 10
        assertFalse(isOverdueForReview(stagedAt, reviewWindowDays = 30, now = NOW))
        assertTrue(isOverdueForReview(stagedAt, reviewWindowDays = 7, now = NOW))
    }
}
