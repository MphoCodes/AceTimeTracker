package com.example.acetimetracker.ui.activity

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.viewmodel.TimesheetViewModel
import com.example.acetimetracker.util.Resource
import java.util.Calendar
import java.util.Locale

class TimesheetActivity : AppCompatActivity() {

    private val viewModel: TimesheetViewModel by viewModels()
    private val categoryNames = ArrayList<String>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var selectedImageView: ImageView

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageView.visibility = View.VISIBLE
                    Glide.with(this).load(uri).into(selectedImageView)
                    viewModel.uploadImage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timesheet)

        val spinnerCategory: Spinner = findViewById(R.id.spinnerCategory)
        val buttonUploadImage: ImageButton = findViewById(R.id.buttonUploadImage)
        val buttonSave: Button = findViewById(R.id.buttonSave)
        selectedImageView = findViewById(R.id.imageViewSelectedImage)

        val editTextStartDate: EditText = findViewById(R.id.editTextStartDate)
        val editTextStartTime: EditText = findViewById(R.id.editTextStartTime)
        val editTextEndDate: EditText = findViewById(R.id.editTextEndDate)
        val editTextEndTime: EditText = findViewById(R.id.editTextEndTime)
        val editTextDescription: EditText = findViewById(R.id.editTextDescription)

        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        setupDateAndTimePickers(editTextStartDate, editTextEndDate, editTextStartTime, editTextEndTime)

        buttonUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            imagePickerLauncher.launch(intent)
        }

        buttonSave.setOnClickListener {
            val category = spinnerCategory.selectedItem?.toString().orEmpty()
            val startDate = editTextStartDate.text.toString().trim()
            val startTime = editTextStartTime.text.toString().trim()
            val endDate = editTextEndDate.text.toString().trim()
            val endTime = editTextEndTime.text.toString().trim()
            val description = editTextDescription.text.toString().trim()

            viewModel.saveTimesheetEntry(
                category = category,
                startDate = startDate,
                startTime = startTime,
                endDate = endDate,
                endTime = endTime,
                description = description
            )
        }

        observeViewModel(
            spinnerCategory,
            editTextStartDate,
            editTextStartTime,
            editTextEndDate,
            editTextEndTime,
            editTextDescription
        )

        viewModel.loadCategories()
    }

    private fun observeViewModel(
        spinner: Spinner,
        startDateEdit: EditText,
        startTimeEdit: EditText,
        endDateEdit: EditText,
        endTimeEdit: EditText,
        descEdit: EditText
    ) {
        viewModel.categoriesState.observe(this) { resource ->
            if (resource is Resource.Success) {
                categoryNames.clear()
                categoryNames.add("Select Category")
                resource.data.forEach { categoryNames.add(it.name) }
                spinnerAdapter.notifyDataSetChanged()
            }
        }

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
                    startDateEdit.text.clear()
                    startTimeEdit.text.clear()
                    endDateEdit.text.clear()
                    endTimeEdit.text.clear()
                    descEdit.text.clear()
                    spinner.setSelection(0)
                    selectedImageView.visibility = View.GONE
                    viewModel.selectedImageUrl = ""
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupDateAndTimePickers(
        startDate: EditText,
        endDate: EditText,
        startTime: EditText,
        endTime: EditText
    ) {
        val dateListener = { target: EditText ->
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val timeListener = { target: EditText ->
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    target.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        startDate.setOnClickListener { dateListener(startDate) }
        endDate.setOnClickListener { dateListener(endDate) }
        startTime.setOnClickListener { timeListener(startTime) }
        endTime.setOnClickListener { timeListener(endTime) }
    }
}
