package com.example.acetimetracker.util

import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Characterizes supported same-day formatting; date/overnight changes belong to P8. */
@RunWith(Parameterized::class)
class TimeUtilsCharacterizationTest(
    private val start: String,
    private val end: String,
    private val expected: String,
) {
    private lateinit var previousLocale: Locale
    private lateinit var previousTimeZone: TimeZone

    @Before
    fun useDeterministicLocaleAndTimeZone() {
        previousLocale = Locale.getDefault()
        previousTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreLocaleAndTimeZone() {
        Locale.setDefault(previousLocale)
        TimeZone.setDefault(previousTimeZone)
    }

    @Test
    fun formatsSameDayDurationWithoutLosingMinutes() {
        assertEquals(expected, TimeUtils.calculateTimeDifference(start, end))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} to {1} = {2}")
        fun durations(): List<Array<String>> = listOf(
            arrayOf("09:15", "09:45", "00:30"),
            arrayOf("08:45", "10:00", "01:15"),
            arrayOf("00:00", "00:01", "00:01"),
            arrayOf("00:00", "23:59", "23:59"),
            arrayOf("12:00", "12:00", "00:00"),
        )
    }
}
