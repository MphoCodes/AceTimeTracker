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
    @Suppress("DEPRECATION")
    private fun export(entry: TimesheetEntry): String {
        val activity = Robolectric.buildActivity(TimesheetActivity::class.java).get()
        val method = TimesheetActivity::class.java.getDeclaredMethod("exportAndShareCsv", List::class.java)
        method.isAccessible = true
        method.invoke(activity, listOf(entry))
        val chooser = shadowOf(activity).nextStartedActivity
        val share = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        return share.getStringExtra(Intent.EXTRA_TEXT)!!
    }

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
