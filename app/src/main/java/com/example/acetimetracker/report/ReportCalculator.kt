package com.example.acetimetracker.report

import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Calendar dates and session clock times are local, matching the stored entry format. */
data class ReportRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) }
    val days: Long get() = ChronoUnit.DAYS.between(start, endInclusive) + 1
    fun previous() = ReportRange(start.minusDays(days), start.minusDays(1))

    companion object {
        fun parse(start: String, end: String): ReportRange? {
            val from = parseReportDate(start) ?: return null
            val to = parseReportDate(end) ?: return null
            return if (to < from) null else ReportRange(from, to)
        }
        fun week(today: LocalDate): ReportRange {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return ReportRange(monday, monday.plusDays(6))
        }
        fun month(today: LocalDate) = ReportRange(today.withDayOfMonth(1), today.with(TemporalAdjusters.lastDayOfMonth()))
    }
}

fun parseReportDate(value: String): LocalDate? =
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) null
    else runCatching { LocalDate.parse(value).takeIf { it.year > 0 } }.getOrNull()

data class ReportSession(
    val entry: TimesheetEntry,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val trackedMillis: Long,
)

data class CategoryReport(val name: String, val trackedMillis: Long, val goalHours: Double?) {
    val actualHours: Double get() = trackedMillis / 3_600_000.0
    val goalFraction: Float? get() = goalHours?.let { (actualHours / it).coerceIn(0.0, 1.0).toFloat() }
}

data class ReportSummary(
    val range: ReportRange,
    val sessions: List<ReportSession>,
    val totalMillis: Long,
    val previousMillis: Long,
    val categories: List<CategoryReport>,
    val invalidEntryCount: Int,
) {
    val changePercent: Double? get() = previousMillis.takeIf { it > 0L }?.let {
        (totalMillis.toDouble() - it) / it * 100.0
    }
}

data class TimelineSession(val session: ReportSession, val overlapsPrevious: Boolean)
data class TimelineGap(val start: LocalDateTime, val end: LocalDateTime) {
    val millis: Long get() = Duration.between(start, end).toMillis()
}
data class DailyTimeline(val day: LocalDate, val sessions: List<TimelineSession>, val gaps: List<TimelineGap>, val overlapMillis: Long)

object ReportCalculator {
    fun summarize(entries: List<TimesheetEntry>, categories: List<Category>, range: ReportRange): ReportSummary {
        val valid = entries.mapNotNull(::normalize)
        val sessions = valid.mapNotNull { clip(it, range) }.sortedBy { it.start }
        val totals = sessions.groupBy { it.entry.category }.mapValues { (_, values) -> values.sumOf { it.trackedMillis } }
        val goals = categories.associateBy { it.name }
        val rows = (goals.keys + totals.keys).map { name ->
            CategoryReport(name, totals[name] ?: 0L, goals[name]?.goal?.takeIf { it.isFinite() && it > 0f }?.toDouble())
        }.sortedByDescending { it.trackedMillis }
        return ReportSummary(range, sessions, sessions.sumOf { it.trackedMillis },
            valid.mapNotNull { clip(it, range.previous()) }.sumOf { it.trackedMillis }, rows, entries.size - valid.size)
    }

    fun timeline(entries: List<TimesheetEntry>, day: LocalDate): DailyTimeline {
        val sessions = entries.mapNotNull(::normalize).mapNotNull { clip(it, ReportRange(day, day)) }.sortedBy { it.start }
        var coveredUntil: LocalDateTime? = null
        val gaps = mutableListOf<TimelineGap>()
        val timelineSessions = sessions.map { session ->
            val previous = coveredUntil
            if (previous != null && session.start > previous) gaps += TimelineGap(previous, session.start)
            val overlaps = previous != null && session.start < previous
            coveredUntil = maxOf(previous ?: session.end, session.end)
            TimelineSession(session, overlaps)
        }
        val events = sessions.flatMap { listOf(it.start to 1, it.end to -1) }.groupBy({ it.first }, { it.second }).toSortedMap()
        var active = 0
        var last: LocalDateTime? = null
        var overlapping = 0L
        events.forEach { (time, changes) ->
            if (active > 1) overlapping += Duration.between(last, time).toMillis()
            active += changes.sum()
            last = time
        }
        return DailyTimeline(day, timelineSessions, gaps, overlapping)
    }

    private fun normalize(entry: TimesheetEntry): ReportSession? {
        if (entry.timeSpent < 0L) return null
        val startDate = parseReportDate(entry.startDate) ?: return null
        val endDate = if (entry.endDate.isBlank()) startDate else parseReportDate(entry.endDate) ?: return null
        fun time(value: String): LocalTime? = if (Regex("[0-9]{2}:[0-9]{2}").matches(value))
            runCatching { LocalTime.parse(value) }.getOrNull() else null
        val start = startDate.atTime(time(entry.startTime) ?: return null)
        var end = endDate.atTime(time(entry.endTime) ?: return null)
        if (entry.endDate.isBlank() && end < start) end = end.plusDays(1)
        if (end < start) return null
        var span = Duration.between(start, end).toMillis()
        // Timer timestamps have minute resolution; retain sub-minute recorded time.
        if (entry.timeSpent > span && entry.timeSpent - span < 60_000L) {
            end = start.plusSeconds(entry.timeSpent / 1_000L).plusNanos(entry.timeSpent % 1_000L * 1_000_000L)
            span = entry.timeSpent
        }
        if (span == 0L) return if (entry.timeSpent == 0L) ReportSession(entry, start, end, 0L) else null
        return ReportSession(entry, start, end, entry.timeSpent.coerceAtMost(span))
    }

    private fun clip(session: ReportSession, range: ReportRange): ReportSession? {
        val lower = range.start.atStartOfDay()
        val upper = range.endInclusive.plusDays(1).atStartOfDay()
        if (session.start == session.end) return session.takeIf { it.start >= lower && it.start < upper }
        val start = maxOf(lower, session.start)
        val end = minOf(upper, session.end)
        if (end <= start) return null
        val span = Duration.between(session.start, session.end).toMillis()
        fun cumulative(at: LocalDateTime): Long = BigInteger.valueOf(session.trackedMillis)
            .multiply(BigInteger.valueOf(Duration.between(session.start, at).toMillis()))
            .divide(BigInteger.valueOf(span)).toLong()
        return session.copy(start = start, end = end, trackedMillis = cumulative(end) - cumulative(start))
    }
}
