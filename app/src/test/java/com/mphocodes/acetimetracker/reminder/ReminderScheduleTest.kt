package com.mphocodes.acetimetracker.reminder

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZonedDateTime

class ReminderScheduleTest {
    @Test fun disabledAndEmptyDaysDoNotSchedule() {
        val now = ZonedDateTime.parse("2026-11-16T09:00:00+02:00[Africa/Johannesburg]")
        assertNull(ReminderSchedule().nextOccurrence(now))
        assertNull(ReminderSchedule(enabled = true, days = emptySet()).nextOccurrence(now))
    }

    @Test fun choosesTodayBeforeTimeAndTomorrowAfterTime() {
        val schedule = ReminderSchedule(enabled = true)
        val before = ZonedDateTime.parse("2026-11-16T09:00:00+02:00[Africa/Johannesburg]")
        assertEquals(before.withHour(17), schedule.nextOccurrence(before))
        assertEquals(before.plusDays(1).withHour(17), schedule.nextOccurrence(before.withHour(18)))
    }

    @Test fun exactlyAtTimeDoesNotImmediatelyFireAgain() {
        val now = ZonedDateTime.parse("2026-11-16T17:00:00+02:00[Africa/Johannesburg]")
        assertEquals(now.plusDays(1), ReminderSchedule(enabled = true).nextOccurrence(now))
    }

    @Test fun skipsUnselectedDaysAndWrapsToNextWeek() {
        val friday = ZonedDateTime.parse("2026-11-20T18:00:00+02:00[Africa/Johannesburg]")
        val weekdays = ReminderSchedule(enabled = true)
        assertEquals(friday.plusDays(3).withHour(17), weekdays.nextOccurrence(friday))
        val fridayOnly = weekdays.copy(days = setOf(DayOfWeek.FRIDAY))
        assertEquals(friday.plusDays(7).withHour(17), fridayOnly.nextOccurrence(friday))
    }

    @Test fun springDstGapMovesToValidWallTime() {
        val now = ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]")
        val schedule = ReminderSchedule(true, 2, 30, setOf(DayOfWeek.SUNDAY))
        assertEquals(ZonedDateTime.parse("2026-03-08T03:30:00-04:00[America/New_York]"), schedule.nextOccurrence(now))
    }

    @Test fun autumnDstOverlapFiresOnlyOncePerDay() {
        val now = ZonedDateTime.parse("2026-11-01T01:45:00-04:00[America/New_York]")
        val schedule = ReminderSchedule(true, 1, 30, setOf(DayOfWeek.SUNDAY))
        assertEquals(ZonedDateTime.parse("2026-11-08T01:30:00-05:00[America/New_York]"), schedule.nextOccurrence(now))
    }
}
