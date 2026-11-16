package com.mphocodes.acetimetracker.reminder

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

data class ReminderSchedule(
    val enabled: Boolean = false,
    val hour: Int = 17,
    val minute: Int = 0,
    val days: Set<DayOfWeek> = DayOfWeek.values().toList().take(5).toSet()
) {
    init {
        require(hour in 0..23 && minute in 0..59)
    }

    /** Wall-clock time follows the device zone; DST gaps advance to the first valid time. */
    fun nextOccurrence(after: ZonedDateTime): ZonedDateTime? {
        if (!enabled || days.isEmpty()) return null
        for (offset in 0L..7L) {
            val date = after.toLocalDate().plusDays(offset)
            if (date.dayOfWeek !in days) continue
            val candidate = date.atTime(LocalTime.of(hour, minute)).atZone(after.zone)
            if (candidate.isAfter(after)) return candidate
        }
        return null
    }
}
