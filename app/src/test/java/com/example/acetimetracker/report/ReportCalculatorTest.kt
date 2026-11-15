package com.example.acetimetracker.report

import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportCalculatorTest {
    private val day = LocalDate.of(2026, 11, 15)
    private val range = ReportRange(day, day)
    private fun entry(start: String = "09:00", end: String = "10:00", millis: Long = 3_600_000) =
        TimesheetEntry(id = start, category = "Development", startDate = day.toString(),
            startTime = start, endDate = day.toString(), endTime = end, timeSpent = millis)

    @Test fun `custom range validates strict dates and ordering`() {
        assertNull(ReportRange.parse("2026-02-30", "2026-03-01"))
        assertNull(ReportRange.parse("2026-11-16", "2026-11-15"))
        assertNull(ReportRange.parse("2026-1-1", "2026-11-15"))
        assertEquals(range, ReportRange.parse(day.toString(), day.toString()))
    }

    @Test fun `week compares preceding full Monday to Sunday period`() {
        val week = ReportRange.week(day)
        assertEquals(LocalDate.of(2026, 11, 9), week.start)
        assertEquals(LocalDate.of(2026, 11, 15), week.endInclusive)
        assertEquals(ReportRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8)), week.previous())
    }

    @Test fun `prior custom period has equal inclusive length across month boundary`() {
        val selected = ReportRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3))
        assertEquals(ReportRange(LocalDate.of(2026, 2, 26), LocalDate.of(2026, 2, 28)), selected.previous())
    }

    @Test fun `empty report has no undefined percentage`() {
        val report = ReportCalculator.summarize(emptyList(), emptyList(), range)
        assertEquals(0L, report.totalMillis)
        assertNull(report.changePercent)
        assertTrue(report.sessions.isEmpty())
    }

    @Test fun `date boundaries clip overnight sessions and exclude end midnight`() {
        val overnight = entry("23:00", "01:00", 7_200_000).copy(startDate = day.minusDays(1).toString())
        val midnight = entry("23:00", "00:00").copy(startDate = day.minusDays(1).toString())
        val future = entry().copy(startDate = day.plusDays(1).toString(), endDate = day.plusDays(1).toString())
        val report = ReportCalculator.summarize(listOf(overnight, midnight, future), emptyList(), range)
        assertEquals(3_600_000L, report.totalMillis)
        assertEquals(1, report.sessions.size)
        assertEquals(day.atStartOfDay(), report.sessions.single().start)
    }

    @Test fun `paused timer time is allocated proportionally across midnight`() {
        val overnight = entry("23:00", "01:00", 3_600_000).copy(startDate = day.minusDays(1).toString())
        assertEquals(1_800_000L, ReportCalculator.summarize(listOf(overnight), emptyList(), range).totalMillis)
    }

    @Test fun `short timer within a single clock minute remains counted`() {
        val report = ReportCalculator.summarize(listOf(entry("09:00", "09:00", 42_000)), emptyList(), range)
        assertEquals(42_000L, report.totalMillis)
        assertEquals(0, report.invalidEntryCount)
    }

    @Test fun `malformed negative and reversed entries are excluded and impossible durations capped`() {
        val entries = listOf(entry().copy(startDate = "2026-02-30"), entry().copy(startTime = "25:00"),
            entry(millis = -1), entry("11:00", "10:00"), entry(millis = Long.MAX_VALUE))
        val report = ReportCalculator.summarize(entries, emptyList(), range)
        assertEquals(3_600_000L, report.totalMillis)
        assertEquals(4, report.invalidEntryCount)
    }

    @Test fun `prior total and finite category goals use selected range`() {
        val report = ReportCalculator.summarize(listOf(entry(), entry(millis = 1_800_000).copy(
            startDate = day.minusDays(1).toString(), endDate = day.minusDays(1).toString())),
            listOf(Category(name = "Development", goal = 2f), Category(name = "Bad", goal = Float.NaN)), range)
        assertEquals(100.0, report.changePercent!!, 0.001)
        assertEquals(2.0, report.categories.first { it.name == "Development" }.goalHours!!, 0.001)
        assertNull(report.categories.first { it.name == "Bad" }.goalHours)
    }

    @Test fun `timeline identifies union gaps without false gap after nested overlap`() {
        val timeline = ReportCalculator.timeline(listOf(entry("09:00", "12:00", 10_800_000),
            entry("10:00", "11:00"), entry("13:00", "14:00")), day)
        assertEquals(3, timeline.sessions.size)
        assertEquals(1, timeline.gaps.size)
        assertEquals(day.atTime(12, 0), timeline.gaps.single().start)
        assertEquals(3_600_000L, timeline.overlapMillis)
        assertTrue(timeline.sessions[1].overlapsPrevious)
        assertFalse(timeline.sessions[2].overlapsPrevious)
    }

    @Test fun `timeline clips overnight at selected day boundaries`() {
        val timeline = ReportCalculator.timeline(listOf(entry("23:00", "02:00", 10_800_000)
            .copy(startDate = day.minusDays(1).toString())), day)
        assertEquals(day.atStartOfDay(), timeline.sessions.single().session.start)
        assertEquals(day.atTime(2, 0), timeline.sessions.single().session.end)
    }

    @Test fun `csv exports clipped sessions with spreadsheet safe text`() {
        val report = ReportCalculator.summarize(listOf(entry().copy(category = "=SUM(A1)", description = "A,\"B\"\nC")), emptyList(), range)
        val csv = ReportCsv.export(report)
        assertTrue(csv.contains("\"'=SUM(A1)\""))
        assertTrue(csv.contains("\"A,\"\"B\"\"\nC\""))
    }
}
