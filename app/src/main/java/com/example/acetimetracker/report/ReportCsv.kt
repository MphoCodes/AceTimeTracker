package com.example.acetimetracker.report

import com.example.acetimetracker.util.TimesheetCsv

object ReportCsv {
    fun export(report: ReportSummary): String = TimesheetCsv.export(report.sessions.map { session ->
        session.entry.copy(startDate = session.start.toLocalDate().toString(), startTime = session.start.toLocalTime().toString(),
            endDate = session.end.toLocalDate().toString(), endTime = session.end.toLocalTime().toString(), timeSpent = session.trackedMillis)
    })
}
