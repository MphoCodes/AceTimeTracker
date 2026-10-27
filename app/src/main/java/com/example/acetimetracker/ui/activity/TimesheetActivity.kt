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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.acetimetracker.ui.screen.TimesheetScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.TimesheetViewModel
import com.example.acetimetracker.util.Resource

class TimesheetActivity : ComponentActivity() {

    private val viewModel: TimesheetViewModel by viewModels()
    private var selectedImageUri by mutableStateOf<Uri?>(null)

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
                val saveEntryState by viewModel.saveEntryState.observeAsState()
                val uploadImageState by viewModel.uploadImageState.observeAsState()

                TimesheetScreen(
                    categoriesState = categoriesState,
                    saveEntryState = saveEntryState,
                    uploadImageState = uploadImageState,
                    selectedImageUri = selectedImageUri,
                    onBackClick = { finish() },
                    onSelectImageClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        imagePickerLauncher.launch(intent)
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
                    }
                )
            }
        }

        viewModel.loadCategories()
    }

    private fun observeViewModel() {
        viewModel.uploadImageState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.saveEntryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Timesheet entry saved successfully!", Toast.LENGTH_SHORT).show()
                    selectedImageUri = null
                    viewModel.selectedImageUrl = ""
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
