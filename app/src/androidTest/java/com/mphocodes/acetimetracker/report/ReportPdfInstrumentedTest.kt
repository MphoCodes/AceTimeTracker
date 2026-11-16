package com.mphocodes.acetimetracker.report

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** PdfDocument's native writer is exercised on Android; Robolectric does not implement it. */
@RunWith(AndroidJUnit4::class)
class ReportPdfInstrumentedTest {
    @Test fun longReportProducesReadablePages() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val day = LocalDate.of(2026, 11, 15)
        val entry = TimesheetEntry(category = "Development", startDate = day.toString(), startTime = "09:00",
            endDate = day.toString(), endTime = "10:00", timeSpent = 3_600_000,
            description = "A detailed session note with commas, quotes and enough text to wrap safely across lines. ".repeat(12))
        val report = ReportCalculator.summarize(List(30) { entry.copy(id = "session-$it") }, emptyList(), ReportRange(day, day))
        val file = ReportFiles.create(context, report, ReportFormat.PDF)
        try {
            assertTrue(file.length() > 1000)
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
                assertTrue("Long reports must paginate rather than clip sessions", pdf.pageCount > 1)
                pdf.openPage(pdf.pageCount - 1).use { page ->
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        assertTrue("Last page must contain rendered content", pixels.count { it != Color.WHITE } > 100)
                    } finally { bitmap.recycle() }
                }
            }
        } finally { file.delete() }
    }

    @Test fun emptyReportProducesReadableDocument() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val day = LocalDate.of(2026, 11, 15)
        val report = ReportCalculator.summarize(emptyList(), emptyList(), ReportRange(day, day))
        val file = ReportFiles.create(context, report, ReportFormat.PDF)
        try {
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
                assertTrue(pdf.pageCount > 0)
            }
        } finally { file.delete() }
    }
}
