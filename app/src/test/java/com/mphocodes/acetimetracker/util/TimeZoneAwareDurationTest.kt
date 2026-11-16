package com.mphocodes.acetimetracker.util

import com.mphocodes.acetimetracker.AceTimeTrackerApp
import com.mphocodes.acetimetracker.data.local.RecordCodec
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric supplies org.json for the codec round-trip case.
@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
class TimeZoneAwareDurationTest {
    private val hour = 60L * 60L * 1000L

    @Test fun springForwardGapYieldsRealElapsedTime() {
        // New York skips 02:00-03:00 on 2026-03-08, so 01:30 -> 03:30 is one real hour.
        val millis = TimeUtils.calculateTimeSpentMillis("2026-03-08", "01:30", "2026-03-08", "03:30", "America/New_York")
        assertEquals(hour, millis)
    }

    @Test fun fallBackOverlapYieldsRealElapsedTime() {
        // 00:30 -> 03:30 across the 2026-11-01 fall-back (01:00 repeats) spans four real hours.
        val millis = TimeUtils.calculateTimeSpentMillis("2026-11-01", "00:30", "2026-11-01", "03:30", "America/New_York")
        assertEquals(4 * hour, millis)
    }

    @Test fun fixedOffsetZoneIgnoresDeviceZone() {
        val millis = TimeUtils.calculateTimeSpentMillis("2026-03-08", "01:30", "2026-03-08", "03:30", "Africa/Johannesburg")
        assertEquals(2 * hour, millis)
    }

    @Test fun unknownZoneFallsBackInsteadOfFailing() {
        val millis = TimeUtils.calculateTimeSpentMillis("2026-06-01", "09:00", "2026-06-01", "10:00", "Mars/Olympus")
        assertEquals(hour, millis)
    }

    @Test fun editKeepsOriginalZoneAndNewEntriesCaptureDeviceZone() {
        val original = TimesheetEntry(id = "e1", userId = "u", category = "Work",
            startDate = "2026-03-08", startTime = "01:30", endDate = "2026-03-08", endTime = "02:30",
            timeSpent = hour, zoneId = "Africa/Johannesburg")
        val edited = EntryEditor.build(original, "u", "Work", "2026-03-08", "01:30", "2026-03-08", "03:30", "", "")
        assertEquals("Africa/Johannesburg", edited.zoneId)
        assertEquals(2 * hour, edited.timeSpent)

        val fresh = EntryEditor.build(null, "u", "Work", "2026-06-01", "09:00", "2026-06-01", "10:00", "", "")
        assertEquals(TimeUtils.currentZoneId(), fresh.zoneId)
    }

    @Test fun codecRoundTripsZoneAndToleratesLegacyPayloads() {
        val entry = TimesheetEntry(id = "e1", userId = "u", category = "Work", startDate = "2026-06-01",
            startTime = "09:00", endDate = "2026-06-01", endTime = "10:00", timeSpent = hour, zoneId = "Europe/London")
        assertEquals(entry, RecordCodec.entry(RecordCodec.encode(entry)))

        val legacy = """{"id":"e0","userId":"u","category":"Work","startDate":"2026-06-01","startTime":"09:00","endDate":"2026-06-01","endTime":"10:00","timeSpent":3600000,"imageUrl":"","description":""}"""
        assertEquals("", RecordCodec.entry(legacy).zoneId)
    }
}
