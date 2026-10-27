package com.example.acetimetracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    private val hour = 60 * 60 * 1000L

    @Test
    fun sameDayDurationIsEndMinusStart() {
        assertEquals(2 * hour + 30 * 60 * 1000L, TimeUtils.calculateTimeSpentMillis("2026-10-27", "09:00", "2026-10-27", "11:30"))
    }

    @Test
    fun overnightSessionUsesDates() {
        assertEquals(3 * hour, TimeUtils.calculateTimeSpentMillis("2026-10-26", "23:00", "2026-10-27", "02:00"))
    }

    @Test
    fun endBeforeStartIsNegative() {
        assertTrue(TimeUtils.calculateTimeSpentMillis("2026-10-27", "10:00", "2026-10-27", "09:00") < 0L)
    }

    @Test
    fun unparseableInputYieldsZero() {
        assertEquals(0L, TimeUtils.calculateTimeSpentMillis("", "10:00", "2026-10-27", "11:00"))
    }

    @Test
    fun validatesRealCalendarValues() {
        assertTrue(TimeUtils.isValidDate("2024-02-29"))
        assertFalse(TimeUtils.isValidDate("2026-02-30"))
        assertFalse(TimeUtils.isValidDate("2026-13-01"))
        assertTrue(TimeUtils.isValidTime("23:59"))
        assertFalse(TimeUtils.isValidTime("24:30"))
        assertFalse(TimeUtils.isValidTime("9:00"))
    }

    @Test
    fun shortDurationFormatting() {
        assertEquals("0m", TimeUtils.formatDurationShort(0L))
        assertEquals("45m", TimeUtils.formatDurationShort(45 * 60 * 1000L))
        assertEquals("2h", TimeUtils.formatDurationShort(2 * hour))
        assertEquals("1h 5m", TimeUtils.formatDurationShort(hour + 5 * 60 * 1000L))
    }

    @Test
    fun formatDurationClampsNegative() {
        assertEquals("00:00:00", TimeUtils.formatDuration(-5000L))
    }

    @Test
    fun startOfWeekIsMonday() {
        val sunday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 1, 12, 0) }.time
        assertEquals("2026-10-26", TimeUtils.startOfWeek(sunday))
        val monday = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 26, 8, 0) }.time
        assertEquals("2026-10-26", TimeUtils.startOfWeek(monday))
    }
}
