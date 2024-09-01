package com.example.acetimetracker

import android.content.ContentValues.TAG
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Myprogress : AppCompatActivity() {
    private lateinit var categorySpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var timesheetAdapter: TimesheetAdapter
    private lateinit var totalAllocatedTimeTextView: TextView
    private lateinit var barChart: BarChart
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val DATE_FORMAT = "dd/MM"
        const val GOAL_PERIOD_DAYS = 14
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_myprogress)

        recyclerView = findViewById(R.id.progRecyclerView)
        totalAllocatedTimeTextView = findViewById(R.id.totalAllocatedTimeTextView)
        barChart = findViewById(R.id.progressBarChart)

        categorySpinner = findViewById(R.id.categorySpinner)
        fetchCategories()

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = categorySpinner.selectedItem.toString()
                if (position != 0) {
                    timesheetAdapter.filterByCategory(selectedCategory)
                    fetchAndDisplayProgressData(selectedCategory)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle no selection case
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        timesheetAdapter = TimesheetAdapter(this)
        recyclerView.adapter = timesheetAdapter

        fetchTimesheets()
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = timeInMillis / (1000 * 60 * 60)
        val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (timeInMillis % (1000 * 60)) / 1000

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun fetchCategories() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d(TAG, "Fetching categories for user: $userId")

            firestore.collection("users").document(userId)
                .collection("categories")
                .get()
                .addOnSuccessListener { documents ->
                    val fetchedCategories = mutableListOf<String>("Select Category")
                    Log.d(TAG, "Fetched ${documents.size()} categories")

                    for (document in documents) {
                        val category = document.getString("name")
                        Log.d(TAG, "Fetched category: $category")
                        if (category != null) {
                            fetchedCategories.add(category)
                        }
                    }

                    val adapter =
                        ArrayAdapter(this, android.R.layout.simple_spinner_item, fetchedCategories)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    categorySpinner.adapter = adapter

                    if (fetchedCategories.size > 1) {
                        categorySpinner.setSelection(1)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to fetch categories: ${exception.message}", exception)
                    Toast.makeText(
                        this,
                        "Failed to fetch categories: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Log.e(TAG, "User not logged in.")
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAndDisplayProgressData(selectedCategory: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d(TAG, "Fetching progress data for category: $selectedCategory and user: $userId")

            firestore.collection("users").document(userId)
                .collection("categories")
                .whereEqualTo("name", selectedCategory)
                .get()
                .addOnSuccessListener { categoryDocuments ->
                    if (categoryDocuments.isEmpty) {
                        Log.w(TAG, "No category document found for: $selectedCategory")
                        return@addOnSuccessListener
                    }

                    val categoryDocument = categoryDocuments.documents[0]
                    val categoryGoal = categoryDocument.getDouble("goal")?.toFloat() ?: 0f
                    Log.d(TAG, "Category goal for $selectedCategory: $categoryGoal")

                    firestore.collection("timesheetEntries")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("category", selectedCategory)
                        .orderBy("startDate")
                        .get()
                        .addOnSuccessListener { timesheetDocuments ->
                            Log.d(TAG, "Fetched ${timesheetDocuments.size()} timesheet entries")

                            val timesheetEntries =
                                timesheetDocuments.toObjects(TimesheetEntry::class.java)
                            updateBarChart(timesheetEntries, categoryGoal) // Call updateBarChart() here
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Error fetching timesheet entries", exception)
                            Toast.makeText(
                                this,
                                "Error loading timesheet entries",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching category goal", exception)
                    Toast.makeText(this, "Error loading category goal", Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBarChart(timesheetEntries: List<TimesheetEntry>, categoryGoal: Float) {
        val entries = mutableListOf<BarEntry>()
        val labels = listOf("Current Progress", "Goal")

        var totalTimeSpent = 0f
        for (entry in timesheetEntries) {
            totalTimeSpent += (entry.timeSpent / (1000 * 60 * 60)).toFloat()
        }

        // Add entries for current progress and goal
        entries.add(BarEntry(0f, totalTimeSpent))
        entries.add(BarEntry(1f, categoryGoal))

        val barDataSet = BarDataSet(entries, "Hours").apply {
            colors = listOf(
                ContextCompat.getColor(this@Myprogress, R.color.red), // Color for current progress
                ContextCompat.getColor(this@Myprogress, R.color.green)  // Color for goal
            )
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        val barData = BarData(barDataSet)
        barChart.data = barData

        // Configure the X-axis
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels) // Set custom labels
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f

        // Other chart settings
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setFitBars(true) // Makes the bars fit to the chart width

        barChart.setTouchEnabled(true)
        barChart.isDragEnabled = true
        barChart.setScaleEnabled(true)

        barChart.invalidate() // Refresh the chart
    }

    private fun fetchTimesheets() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d(TAG, "Fetching timesheets for user: $userId")

            firestore.collection("timesheetEntries")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "Fetched ${documents.size()} timesheet entries")

                    val timesheets = mutableListOf<TimesheetEntry>()
                    var totalAllocatedTime = 0L

                    for (document in documents) {
                        val timesheet = document.toObject(TimesheetEntry::class.java)
                        if (timesheet.timeSpent != null) {
                            timesheets.add(timesheet)
                            totalAllocatedTime += timesheet.timeSpent
                        }
                    }

                    timesheetAdapter.submitList(timesheets)
                    totalAllocatedTimeTextView.text =
                        "Total Allocated Time: ${formatTime(totalAllocatedTime)}"
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to fetch timesheets: ${exception.message}", exception)
                    Toast.makeText(
                        this,
                        "Failed to fetch timesheets: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Log.e(TAG, "User not logged in.")
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}
