package com.mphocodes.acetimetracker.report

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mphocodes.acetimetracker.util.TimeUtils
import java.io.OutputStream
import java.util.Locale

/** A4 report with repeated page headers, wrapped content and a session audit trail. */
object ReportPdf {
    fun write(report: ReportSummary, output: OutputStream) {
        val document = PdfDocument()
        try {
            val writer = Pages(document, report)
            writer.title("Time report")
            writer.line("${report.range.start}  —  ${report.range.endInclusive}", muted = true)
            writer.space(12f)
            writer.metric(TimeUtils.formatDuration(report.totalMillis), "TRACKED TIME · ${report.sessions.size} SESSIONS")
            writer.space(12f)
            val prior = report.range.previous()
            writer.line("Previous period: ${prior.start} — ${prior.endInclusive}")
            writer.line("${TimeUtils.formatDuration(report.previousMillis)} tracked · ${comparison(report)}", muted = true)
            writer.space(18f)
            writer.section("Category goals & actuals")
            writer.line("Targets are configured category goals, compared with this selected period.", muted = true)
            if (report.categories.isEmpty()) writer.line("No categories or sessions in this period.")
            report.categories.forEach { category ->
                writer.space(8f)
                writer.line(category.name.ifBlank { "Uncategorized" }, bold = true)
                val goal = category.goalHours?.let { String.format(Locale.getDefault(), "%.1f h target", it) } ?: "No target set"
                writer.line("${TimeUtils.formatDuration(category.trackedMillis)} actual  /  $goal", muted = true)
                category.goalFraction?.let { writer.progress(it) }
            }
            writer.space(18f)
            writer.section("Session detail")
            if (report.sessions.isEmpty()) writer.line("No sessions in the selected period.")
            report.sessions.forEachIndexed { index, session ->
                writer.ensure(68f)
                writer.space(8f)
                writer.line("${index + 1}. ${session.entry.category.ifBlank { "Uncategorized" }}", bold = true)
                writer.line("${session.start.toLocalDate()} ${session.start.toLocalTime()}  →  ${session.end.toLocalDate()} ${session.end.toLocalTime()}", muted = true)
                writer.line("Recorded: ${TimeUtils.formatDuration(session.trackedMillis)} · ${TimeUtils.resolveZone(session.entry.zoneId).id}")
                if (session.entry.description.isNotBlank()) writer.line(session.entry.description, muted = true)
                writer.rule()
            }
            writer.space(16f)
            writer.section("How to read this report")
            writer.line("Sessions crossing the report boundary are clipped. Recorded time is allocated proportionally when the clock span includes pauses. Overlapping sessions each contribute their recorded time.", muted = true)
            writer.line("Invalid dates and negative durations are omitted. Inconsistent durations are capped to the clock span, allowing for minute-resolution timer timestamps.", muted = true)
            if (report.invalidEntryCount > 0) writer.line("${report.invalidEntryCount} invalid record(s) omitted from source data.", muted = true)
            writer.finish()
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun comparison(report: ReportSummary): String = report.changePercent?.let {
        String.format(Locale.getDefault(), "%+.1f%% change", it)
    } ?: if (report.totalMillis > 0) "No previous tracked time" else "No tracked time in either period"

    private class Pages(private val document: PdfDocument, private val report: ReportSummary) {
        private val ink = Color.rgb(31, 41, 55)
        private val muted = Color.rgb(100, 116, 139)
        private val accent = Color.rgb(220, 38, 38)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = 0f
        private val canvas get() = requireNotNull(page).canvas

        init { newPage() }

        private fun newPage() {
            page?.let { finishPage(it) }
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            paint.color = accent
            canvas.drawRect(0f, 0f, 595f, 7f, paint)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ACE TIME TRACKER", 40f, 40f, paint)
            paint.color = muted
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("${report.range.start} — ${report.range.endInclusive}", 355f, 40f, paint)
            y = 78f
        }

        private fun finishPage(current: PdfDocument.Page) {
            paint.color = muted
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            current.canvas.drawText("AceTimeTracker  ·  Personal time report", 40f, 810f, paint)
            current.canvas.drawText("Page $pageNumber", 502f, 810f, paint)
            document.finishPage(current)
        }

        fun ensure(height: Float) { if (y + height > 776f) newPage() }
        fun space(height: Float) { ensure(height); y += height }
        fun title(text: String) = text(text, 28f, ink, true, 36f)
        fun section(text: String) { ensure(60f); text(text, 15f, ink, true, 24f) }
        fun line(text: String, muted: Boolean = false, bold: Boolean = false) =
            text(text, 10f, if (muted) this.muted else ink, bold, 16f)
        fun metric(value: String, label: String) {
            ensure(75f)
            text(value, 32f, accent, true, 40f)
            text(label, 9f, muted, true, 18f)
        }
        fun progress(fraction: Float) {
            ensure(14f)
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(40f, y, 555f, y + 5f, 2.5f, 2.5f, paint)
            paint.color = accent
            canvas.drawRoundRect(40f, y, 40f + 515f * fraction, y + 5f, 2.5f, 2.5f, paint)
            y += 14f
        }
        fun rule() {
            ensure(10f)
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(40f, y + 4f, 555f, y + 5f, paint)
            y += 10f
        }

        private fun text(value: String, size: Float, color: Int, bold: Boolean, lineHeight: Float) {
            value.replace('\t', ' ').split('\n').forEach { paragraph ->
                var remaining = paragraph
                do {
                    ensure(lineHeight)
                    paint.textSize = size
                    paint.color = color
                    paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
                    var count = paint.breakText(remaining, true, 515f, null).coerceAtLeast(1).coerceAtMost(remaining.length)
                    if (count < remaining.length) {
                        val space = remaining.lastIndexOf(' ', count - 1)
                        if (space > 0) count = space
                    }
                    canvas.drawText(remaining.take(count), 40f, y, paint)
                    y += lineHeight
                    remaining = remaining.drop(count).trimStart()
                } while (remaining.isNotEmpty())
            }
        }
        fun finish() { page?.let { finishPage(it) }; page = null }
    }
}
