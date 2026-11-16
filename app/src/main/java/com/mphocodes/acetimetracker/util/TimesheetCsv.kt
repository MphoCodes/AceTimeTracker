package com.mphocodes.acetimetracker.util

import com.mphocodes.acetimetracker.data.model.TimesheetEntry

/** RFC 4180 quoting with spreadsheet formula protection for user-supplied cells. */
object TimesheetCsv {
    fun export(entries: List<TimesheetEntry>): String = buildString {
        append("Category,Start Date,Start Time,End Date,End Time,Duration,Description\r\n")
        entries.forEach { entry ->
            append(listOf(
                entry.category, entry.startDate, entry.startTime,
                entry.endDate, entry.endTime, TimeUtils.formatDuration(entry.timeSpent),
                entry.description
            ).joinToString(",") { cell(it) })
            append("\r\n")
        }
    }

    private fun cell(value: String): String {
        val startsFormula = value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')
        val safe = if (startsFormula || value.firstOrNull() in listOf('\t', '\r', '\n')) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
