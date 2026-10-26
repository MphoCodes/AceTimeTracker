package com.example.acetimetracker.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.viewmodel.ProgressViewModel
import com.example.acetimetracker.util.Resource
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class GraphActivity : AppCompatActivity() {

    private val viewModel: ProgressViewModel by viewModels()
    private lateinit var chart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        chart = findViewById(R.id.chart)

        setupChart()
        observeViewModel()
        viewModel.loadChartData()
    }

    private fun setupChart() {
        chart.setUsePercentValues(true)
        chart.description.isEnabled = false
        chart.centerText = "Time Spent"
        chart.setEntryLabelColor(Color.BLACK)
        chart.setEntryLabelTextSize(12f)
        chart.legend.isEnabled = true
        chart.holeRadius = 40f
        chart.transparentCircleRadius = 45f
    }

    private fun observeViewModel() {
        viewModel.chartDataState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val categoryTimeMap = resource.data
                    val entries = categoryTimeMap.map { (cat, hours) ->
                        PieEntry(hours, cat)
                    }

                    if (entries.isNotEmpty()) {
                        val dataSet = PieDataSet(entries, "Time Distribution").apply {
                            colors = ColorTemplate.MATERIAL_COLORS.toList()
                            valueTextColor = Color.BLACK
                            valueTextSize = 12f
                        }

                        val pieData = PieData(dataSet).apply {
                            setValueFormatter(PercentFormatter(chart))
                            setValueTextSize(12f)
                            setValueTextColor(Color.BLACK)
                        }
                        chart.data = pieData
                        chart.invalidate()
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
