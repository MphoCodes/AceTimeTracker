package com.example.acetimetracker.ui

import android.content.Intent
import com.example.acetimetracker.AceTimeTrackerApp
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.ui.activity.TimesheetActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
class TimesheetExportTest {
    private fun export(entry: TimesheetEntry): String = com.example.acetimetracker.util.TimesheetCsv.export(listOf(entry))

    @Test fun escapesQuotesInCategoryAndDescription() {
        val csv = export(TimesheetEntry(category = "A \"quoted\", category", description = "Line \"one\"\nLine two"))
        assertTrue(csv.contains("\"A \"\"quoted\"\", category\""))
        assertTrue(csv.contains("\"Line \"\"one\"\"\nLine two\""))
    }

    @Test fun neutralizesSpreadsheetFormulasInUserText() {
        val csv = export(TimesheetEntry(category = "=1+1", description = "  @SUM(1,2)"))
        assertTrue(csv.contains("\"'=1+1\""))
        assertTrue(csv.contains("\"'  @SUM(1,2)\""))
    }
}
