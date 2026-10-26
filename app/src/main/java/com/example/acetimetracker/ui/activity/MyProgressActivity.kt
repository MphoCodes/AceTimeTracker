package com.example.acetimetracker.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acetimetracker.R
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.ui.adapter.TimesheetAdapter
import com.example.acetimetracker.ui.viewmodel.ProgressViewModel
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class MyProgressActivity : AppCompatActivity() {

    private val viewModel: ProgressViewModel by viewModels()
    private lateinit var categorySpinner: Spinner
    private lateinit var timesheetAdapter: TimesheetAdapter
    private lateinit var totalAllocatedTimeTextView: TextView
    private lateinit var barChart: BarChart
    private val categoryList = mutableListOf<Category>()
    private val categoryNames = mutableListOf<String>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_myprogress)

        val recyclerView = findViewById<RecyclerView>(R.id.progRecyclerView)
        totalAllocatedTimeTextView = findViewById(R.id.totalAllocatedTimeTextView)
        barChart = findViewById(R.id.progressBarChart)
        categorySpinner = findViewById(R.id.categorySpinner)

        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        timesheetAdapter = TimesheetAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = timesheetAdapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position < categoryNames.size) {
                    val selectedCategory = categoryNames[position]
                    timesheetAdapter.filterByCategory(selectedCategory)
                    updateChartForSelectedCategory(selectedCategory)
                } else {
                    timesheetAdapter.filterByCategory("All")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        observeViewModel()
        viewModel.loadCategories()
        viewModel.loadTimesheets()
    }

    private fun observeViewModel() {
        viewModel.categoriesState.observe(this) { resource ->
            if (resource is Resource.Success) {
                categoryList.clear()
                categoryList.addAll(resource.data)
                categoryNames.clear()
                categoryNames.add("All Categories")
                resource.data.forEach { categoryNames.add(it.name) }
                spinnerAdapter.notifyDataSetChanged()
            }
        }

        viewModel.timesheetsState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val entries = resource.data
                    timesheetAdapter.submitList(entries)

                    val totalMillis = entries.sumOf { it.timeSpent }
                    totalAllocatedTimeTextView.text = "Total Allocated Time: ${TimeUtils.formatDuration(totalMillis)}"
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateChartForSelectedCategory(categoryName: String) {
        val targetCategory = categoryList.find { it.name == categoryName }
        val goalHours = targetCategory?.goal ?: 0f

        val currentEntries = (viewModel.timesheetsState.value as? Resource.Success)?.data.orEmpty()
        val filtered = currentEntries.filter { it.category == categoryName }
        val totalSpentHours = filtered.sumOf { it.timeSpent } / (1000f * 60f * 60f)

        val entries = listOf(
            BarEntry(0f, totalSpentHours),
            BarEntry(1f, goalHours)
        )

        val barDataSet = BarDataSet(entries, "Hours").apply {
            colors = listOf(
                ContextCompat.getColor(this@MyProgressActivity, R.color.red),
                ContextCompat.getColor(this@MyProgressActivity, R.color.green)
            )
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        barChart.data = BarData(barDataSet)
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(listOf("Current Progress", "Goal"))
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
        }
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setFitBars(true)
        barChart.invalidate()
    }
}
