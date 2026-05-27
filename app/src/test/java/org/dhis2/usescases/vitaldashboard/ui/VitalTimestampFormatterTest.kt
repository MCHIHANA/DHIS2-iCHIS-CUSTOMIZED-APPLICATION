package org.dhis2.usescases.vitaldashboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class VitalTimestampFormatterTest {

    @Test
    fun `should label current readings as fresh now`() {
        val now = 1_000_000L

        assertEquals("Fresh now", formatReadingFreshness(now, now))
    }

    @Test
    fun `should label recent readings in minutes`() {
        val now = 10 * 60_000L
        val readingTime = 7 * 60_000L

        assertEquals("3 min ago", formatReadingFreshness(readingTime, now))
    }

    @Test
    fun `should label older readings in days`() {
        val now = 3 * 86_400_000L
        val readingTime = 86_400_000L

        assertEquals("2 days ago", formatReadingFreshness(readingTime, now))
    }
}
