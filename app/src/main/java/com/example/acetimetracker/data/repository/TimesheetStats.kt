package com.example.acetimetracker.data.repository

import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.util.TimeUtils

/** Pure aggregation helpers over saved timesheet entries. */
object TimesheetStats {

    fun hoursByCategory(entries: List<TimesheetEntry>): Map<String, Float> =
        entries.groupBy { it.category }
            .mapValues { (_, list) -> TimeUtils.millisToHours(list.sumOf { it.timeSpent.coerceAtLeast(0L) }) }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

    fun millisOnOrAfter(entries: List<TimesheetEntry>, date: String): Long =
        entries.filter { it.startDate >= date }.sumOf { it.timeSpent.coerceAtLeast(0L) }

    fun millisOn(entries: List<TimesheetEntry>, date: String): Long =
        entries.filter { it.startDate == date }.sumOf { it.timeSpent.coerceAtLeast(0L) }

    /** Number of consecutive days, ending today or yesterday, that have at least one entry. */
    fun currentStreakDays(entries: List<TimesheetEntry>, today: String = TimeUtils.today()): Int {
        val days = entries.map { it.startDate }.toSet()
        val cal = java.util.Calendar.getInstance()
        cal.time = TimeUtils.parseDateTime(today, "00:00") ?: return 0
        if (TimeUtils.formatDate(cal.time) !in days) {
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        var streak = 0
        while (TimeUtils.formatDate(cal.time) in days) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }
}
