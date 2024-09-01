package com.example.acetimetracker

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class Timesheet : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: ArrayAdapter<String>
    private val categoryList = ArrayList<String>()
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 123
    }

    // Use ActivityResultLauncher for image selection
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    // Preview the selected image using Glide
                    Glide.with(this)
                        .load(selectedImageUri)
                        .into(findViewById(R.id.imageViewSelectedImage))

                    // Upload the image to Firebase Storage
                    uploadImageToFirebaseStorage(selectedImageUri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timesheet)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Check if user is authenticated
        if (auth.currentUser == null) {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
            finish() // Close the activity if the user is not authenticated
            return
        }

        // Initialize adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set up spinnerCategory with adapter
        val spinnerCategory: Spinner = findViewById(R.id.spinnerCategory)
        spinnerCategory.adapter = adapter

        setupCategorySpinner()
        setupImageUploadButton()
        setupSaveButton()
        setupDateAndTimePickers()
    }


    private fun setupCategorySpinner() {
        // Clear existing categoryList
        categoryList.clear()

        // Query Firestore for categories
        firestore.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val categoryName = document.getString("name")
                    categoryName?.let { categoryList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle failures
                showToast("Failed to fetch categories: ${exception.message}")
            }
    }

    private fun setupImageUploadButton() {
        val buttonUploadImage: ImageButton = findViewById(R.id.buttonUploadImage)
        buttonUploadImage.setOnClickListener {
            // Implement image upload functionality
            uploadImage()
        }
    }

    private fun uploadImage() {
        // Launch intent to select an image from the device
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startForResult.launch(intent)
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri?) {
        if (imageUri != null) {
            val storageReference = FirebaseStorage.getInstance().reference
            val imageRef = storageReference.child("images/${UUID.randomUUID()}")
            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully, get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Save the download URL to the timesheet entry and show success message
                        selectedImageUri = uri // Update the URI to use later
                        showToast("Image uploaded successfully")
                    }
                }
                .addOnFailureListener { e ->
                    // Handle any errors that occurred during the upload process
                    showToast("Image upload failed: ${e.message}")
                }
        } else {
            showToast("No image selected")
        }
    }

    private fun setupSaveButton() {
        val buttonSave: Button = findViewById(R.id.buttonSave)
        buttonSave.setOnClickListener {
            // Validate input data
            if (isValidInput()) {
                // Save timesheet entry
                saveTimesheetEntry()
            } else {
                // Display error message or prompt user to correct input
            }
        }
    }

    private fun isValidInput(): Boolean {
        val category = findViewById<Spinner>(R.id.spinnerCategory).selectedItem.toString()
        val startDate = findViewById<EditText>(R.id.editTextStartDate).text.toString()
        val startTime = findViewById<EditText>(R.id.editTextStartTime).text.toString()
        val endDate = findViewById<EditText>(R.id.editTextEndDate).text.toString()
        val endTime = findViewById<EditText>(R.id.editTextEndTime).text.toString()
        val description = findViewById<EditText>(R.id.editTextDescription).text.toString()

        // Validate category selection
        if (category.isEmpty()) {
            showToast("Please select a category")
            return false
        }

        // Validate date format and content
        if (!isValidDate(startDate) || !isValidDate(endDate)) {
            showToast("Please enter valid start and end dates (YYYY-MM-DD)")
            return false
        }

        // Validate time format and content
        if (!isValidTime(startTime) || !isValidTime(endTime)) {
            showToast("Please enter valid start and end times (HH:MM)")
            return false
        }

        return true
    }

    private fun formatTimeToMilliseconds(formattedTime: String): Long {
        val parts = formattedTime.split(":")
        require(parts.size == 2) { "Invalid time format" }
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        return (hours * 60 * 60 * 1000) + (minutes * 60 * 1000)
    }

    private fun isValidDate(date: String): Boolean {
        val regex = Regex("^\\d{4}-\\d{2}-\\d{2}\$")
        return regex.matches(date)
    }

    private fun isValidTime(time: String): Boolean {
        val regex = Regex("^\\d{2}:\\d{2}\$")
        return regex.matches(time)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun saveTimesheetEntry() {
        val description = findViewById<EditText>(R.id.editTextDescription).text.toString()
        val category = findViewById<Spinner>(R.id.spinnerCategory).selectedItem.toString()
        val startDate = findViewById<EditText>(R.id.editTextStartDate).text.toString()
        val startTime = findViewById<EditText>(R.id.editTextStartTime).text.toString()
        val endDate = findViewById<EditText>(R.id.editTextEndDate).text.toString()
        val endTime = findViewById<EditText>(R.id.editTextEndTime).text.toString()
        val imageUrl = selectedImageUri?.toString() ?: ""

        val timeSpentInMillis = calculateTimeSpent(startTime, endTime)
        val totalHoursWorked = timeSpentInMillis / (1000 * 60 * 60).toDouble()

        val currentUserId = auth.currentUser?.uid ?: ""

        val entryData = hashMapOf(
            "date" to startDate,
            "totalHours" to totalHoursWorked,
            "category" to category,
            "userId" to currentUserId
        )

        firestore.collection("users").document(currentUserId)
            .update("dailyTimesheetEntries", FieldValue.arrayUnion(entryData))
            .addOnSuccessListener {
                showToast("Timesheet entry saved successfully")
                Log.d("TimesheetActivity", "Timesheet entry saved: $entryData")
                // Clear input fields after successful save
                clearInputFields()
            }
            .addOnFailureListener { e ->
                showToast("Failed to save timesheet entry: ${e.message}")
                Log.e("TimesheetActivity", "Failed to save timesheet entry", e)
            }
    }

    private fun calculateTimeSpent(startTime: String, endTime: String): Long {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return try {
            val startDate = timeFormat.parse(startTime)
            val endDate = timeFormat.parse(endTime)
            endDate.time - startDate.time
        } catch (e: Exception) {
            Log.e("TimesheetActivity", "Error calculating time spent", e)
            0L
        }
    }

    private fun clearInputFields() {
        findViewById<Spinner>(R.id.spinnerCategory).setSelection(0)
        findViewById<EditText>(R.id.editTextStartDate).text.clear()
        findViewById<EditText>(R.id.editTextStartTime).text.clear()
        findViewById<EditText>(R.id.editTextEndDate).text.clear()
        findViewById<EditText>(R.id.editTextEndTime).text.clear()
        findViewById<EditText>(R.id.editTextDescription).text.clear()
        selectedImageUri = null
        findViewById<ImageView>(R.id.imageViewSelectedImage).visibility = ImageView.GONE
    }

    private fun setupDatePicker(editText: EditText) {
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, monthOfYear, dayOfMonth ->
                    // Format the date string to YYYY-MM-DD
                    val formattedDate = String.format("%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
                    editText.setText(formattedDate)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
    }

    private fun setupTimePicker(editText: EditText) {
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    // Format the time string to HH:MM
                    val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                    editText.setText(formattedTime)
                },
                hour,
                minute,
                true // Use 24-hour format
            )
            timePickerDialog.show()
        }
    }

    private fun setupDateAndTimePickers() {
        setupDatePicker(findViewById(R.id.editTextStartDate))
        setupDatePicker(findViewById(R.id.editTextEndDate))
        setupTimePicker(findViewById(R.id.editTextStartTime))
        setupTimePicker(findViewById(R.id.editTextEndTime))
    }


}