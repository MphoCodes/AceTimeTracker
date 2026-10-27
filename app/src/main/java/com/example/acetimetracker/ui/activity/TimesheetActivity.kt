package com.example.acetimetracker.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.ui.screen.TimesheetScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.TimesheetViewModel
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils

class TimesheetActivity : ComponentActivity() {

    private val viewModel: TimesheetViewModel by viewModels()
    private var selectedImageUri by mutableStateOf<Uri?>(null)
    private var formResetKey by mutableIntStateOf(0)

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageUri = uri
                    viewModel.uploadImage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModel()

        setContent {
            AceTimeTrackerTheme {
                val categoriesState by viewModel.categoriesState.observeAsState()
                val timesheetsState by viewModel.timesheetsState.observeAsState()
                val saveEntryState by viewModel.saveEntryState.observeAsState()
                val uploadImageState by viewModel.uploadImageState.observeAsState()

                TimesheetScreen(
                    categoriesState = categoriesState,
                    timesheetsState = timesheetsState,
                    saveEntryState = saveEntryState,
                    uploadImageState = uploadImageState,
                    selectedImageUri = selectedImageUri,
                    formResetKey = formResetKey,
                    onBackClick = { finish() },
                    onSelectImageClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        imagePickerLauncher.launch(intent)
                    },
                    onRemoveImageClick = {
                        selectedImageUri = null
                        viewModel.selectedImageUrl = ""
                    },
                    onSaveClick = { category, startDate, startTime, endDate, endTime, description ->
                        viewModel.saveTimesheetEntry(
                            category = category,
                            startDate = startDate,
                            startTime = startTime,
                            endDate = endDate,
                            endTime = endTime,
                            description = description
                        )
                    },
                    onDeleteEntryClick = { entryId ->
                        viewModel.deleteTimesheet(entryId)
                    },
                    onExportCsvClick = { entries ->
                        exportAndShareCsv(entries)
                    }
                )
            }
        }

        viewModel.loadCategories()
        viewModel.loadTimesheets()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTimesheets()
    }

    private fun exportAndShareCsv(entries: List<TimesheetEntry>) {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No entries to export", Toast.LENGTH_SHORT).show()
            return
        }

        val csvBuilder = StringBuilder()
        csvBuilder.append("Category,Start Date,Start Time,End Date,End Time,Duration,Description\n")

        for (entry in entries) {
            val durationStr = TimeUtils.formatDuration(entry.timeSpent)
            val cleanDesc = entry.description.replace("\"", "\"\"")
            csvBuilder.append("\"${entry.category}\",\"${entry.startDate}\",\"${entry.startTime}\",\"${entry.endDate}\",\"${entry.endTime}\",\"$durationStr\",\"$cleanDesc\"\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AceTimeTracker - Timesheet Report")
            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
        }

        startActivity(Intent.createChooser(shareIntent, "Share Timesheet Report via"))
    }

    private fun observeViewModel() {
        viewModel.uploadImageState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "Photo attached", Toast.LENGTH_SHORT).show()
                    viewModel.consumeUploadState()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    selectedImageUri = null
                    viewModel.consumeUploadState()
                }
                else -> {}
            }
        }

        viewModel.saveEntryState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show()
                    selectedImageUri = null
                    viewModel.selectedImageUrl = ""
                    formResetKey++
                    viewModel.consumeSaveState()
                    viewModel.loadTimesheets()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    viewModel.consumeSaveState()
                }
                else -> {}
            }
        }
    }
}
