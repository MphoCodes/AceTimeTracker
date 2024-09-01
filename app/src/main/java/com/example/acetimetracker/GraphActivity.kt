package com.example.acetimetracker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GraphActivity : AppCompatActivity() {

    private lateinit var chart: PieChart
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        chart = findViewById(R.id.chart) // Assuming you change the ID in the layout
        firestore = FirebaseFirestore.getInstance()

        fetchAndDisplayChartData()
    }

    private fun fetchAndDisplayChartData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val dailyTimesheetEntries = document.get("dailyTimesheetEntries") as List<Map<String, Any>>?

                if (dailyTimesheetEntries != null) {
                    val entries = mutableListOf<PieEntry>()
                    val labels = mutableListOf<String>()
                    val categoryTimeMap = mutableMapOf<String, Float>()

                    for (entry in dailyTimesheetEntries) {
                        val category = entry["category"] as? String ?: continue
                        val totalHours = (entry["totalHours"] as? Number)?.toFloat() ?: continue

                        categoryTimeMap[category] = (categoryTimeMap[category] ?: 0f) + totalHours
                    }

                    categoryTimeMap.forEach { (category, time) ->
                        entries.add(PieEntry(time, category))
                        labels.add(category) // For potential later use
                    }

                    // Create PieDataSet
                    val dataSet = PieDataSet(entries, "Time Distribution").apply {
                        colors = ColorTemplate.MATERIAL_COLORS.toList() // Use your preferred colors
                        valueTextColor = Color.BLACK
                        valueTextSize = 12f
                        // Add any other styling you want here
                    }

                    // Create PieData and set it to the chart
                    val pieData = PieData(dataSet).apply {
                        setValueFormatter(PercentFormatter(chart))
                        setValueTextSize(12f)
                        setValueTextColor(Color.BLACK)
                    }
                    chart.data = pieData

                    // Configure pie chart settings
                    chart.setUsePercentValues(true) // Display values as percentages
                    chart.description.isEnabled = false
                    chart.centerText = "Time Spent"
                    chart.setEntryLabelColor(Color.BLACK)
                    chart.setEntryLabelTextSize(12f)
                    chart.legend.isEnabled = true // Show the legend
                    chart.holeRadius = 40f  // Adjust the hole size if you want
                    chart.transparentCircleRadius = 45f

                    chart.invalidate() // Refresh the chart
                }
            }
            .addOnFailureListener { exception ->
                Log.e("GraphActivity", "Error fetching chart data: ${exception.message}", exception)
            }
    }
}