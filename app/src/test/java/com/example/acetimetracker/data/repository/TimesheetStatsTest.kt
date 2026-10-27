package com.example.acetimetracker.data.repository

import com.example.acetimetracker.data.model.TimesheetEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TimesheetStatsTest {

    private val hour = 60 * 60 * 1000L

    private fun entry(category: String, date: String, millis: Long) =
        TimesheetEntry(category = category, startDate = date, timeSpent = millis)

    @Test
    fun hoursByCategorySumsAndSortsDescending() {
        val result = TimesheetStats.hoursByCategory(
            listOf(
                entry("Reading", "2026-10-27", hour),
                entry("Exercise", "2026-10-27", 3 * hour),
                entry("Reading", "2026-10-26", hour / 2)
            )
        )
        assertEquals(listOf("Exercise", "Reading"), result.keys.toList())
        assertEquals(1.5f, result.getValue("Reading"), 0.001f)
    }

    @Test
    fun identicalSessionsAreBothCounted() {
        val same = entry("Reading", "2026-10-27", hour)
        assertEquals(2f, TimesheetStats.hoursByCategory(listOf(same, same)).getValue("Reading"), 0.001f)
    }

    @Test
    fun periodTotals() {
        val entries = listOf(
            entry("A", "2026-10-20", hour),
            entry("A", "2026-10-26", hour),
            entry("B", "2026-10-27", 2 * hour)
        )
        assertEquals(3 * hour, TimesheetStats.millisOnOrAfter(entries, "2026-10-26"))
        assertEquals(2 * hour, TimesheetStats.millisOn(entries, "2026-10-27"))
    }

    @Test
    fun streakCountsConsecutiveDaysEndingTodayOrYesterday() {
        val entries = listOf(
            entry("A", "2026-10-24", hour),
            entry("A", "2026-10-25", hour),
            entry("A", "2026-10-26", hour)
        )
        assertEquals(3, TimesheetStats.currentStreakDays(entries, today = "2026-10-27"))
        assertEquals(3, TimesheetStats.currentStreakDays(entries, today = "2026-10-26"))
        assertEquals(0, TimesheetStats.currentStreakDays(entries, today = "2026-10-29"))
    }
}
