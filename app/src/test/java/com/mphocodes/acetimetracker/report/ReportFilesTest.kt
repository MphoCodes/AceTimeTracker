package com.mphocodes.acetimetracker.report

import android.app.Application
import android.content.Intent
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class ReportFilesTest {
    private val day = LocalDate.of(2026, 11, 15)
    private val range = ReportRange(day, day)
    private val entry = TimesheetEntry(category = "Development", startDate = day.toString(), startTime = "09:00",
        endDate = day.toString(), endTime = "10:00", timeSpent = 3_600_000,
        description = "A detailed session note with commas, quotes and enough text to wrap safely across lines. ".repeat(12))

    @Test fun `CSV files are UTF8 and chooser grants read access to content URI`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val report = ReportCalculator.summarize(listOf(entry.copy(category = "Étude")), emptyList(), range)
        val file = ReportFiles.create(context, report, ReportFormat.CSV)
        try {
            assertTrue(file.readText(Charsets.UTF_8).contains("Étude"))
            ReportFiles.share(context, file, ReportFormat.CSV)
            val chooser = shadowOf(context).nextStartedActivity
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            @Suppress("DEPRECATION")
            val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals("text/csv", send.type)
            assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals("content", send.clipData!!.getItemAt(0).uri.scheme)
            assertEquals("${context.packageName}.fileprovider", send.clipData!!.getItemAt(0).uri.authority)
        } finally { file.delete() }
    }
}
