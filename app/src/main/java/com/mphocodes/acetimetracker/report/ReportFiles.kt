package com.mphocodes.acetimetracker.report

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ReportFormat(val extension: String, val mime: String) {
    CSV("csv", "text/csv"), PDF("pdf", "application/pdf")
}

object ReportFiles {
    suspend fun create(context: Context, report: ReportSummary, format: ReportFormat): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "reports").apply { check(isDirectory || mkdirs()) }
        val file = File(directory, "AceTimeTracker-${report.range.start}-${report.range.endInclusive}-${UUID.randomUUID()}.${format.extension}")
        try {
            when (format) {
                ReportFormat.CSV -> file.writeText(ReportCsv.export(report), Charsets.UTF_8)
                ReportFormat.PDF -> file.outputStream().use { ReportPdf.write(report, it) }
            }
            file
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    fun share(context: Context, file: File, format: ReportFormat) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Time report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share time report")
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
