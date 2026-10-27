package com.example.acetimetracker.ui.screen

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChartPie
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    chartDataState: Resource<Map<String, Float>>?,
    onBackClick: () -> Unit
) {
    val categoryTimeMap = (chartDataState as? Resource.Success)?.data ?: emptyMap()
    val totalHours = categoryTimeMap.values.sum()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, Slate200)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Visual Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Slate800, modifier = Modifier.size(22.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                chartDataState is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AceRed)
                }
                categoryTimeMap.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AceRedLight.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Lucide.ChartPie,
                                    contentDescription = null,
                                    tint = AceRed,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No analytics data yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Log timesheet entries to visualize your productivity breakdown.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pie Chart Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Time Distribution Across Categories",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp),
                                        factory = { ctx ->
                                            PieChart(ctx).apply {
                                                setUsePercentValues(true)
                                                description.isEnabled = false
                                                centerText = "Time Spent"
                                                setEntryLabelColor(AndroidColor.BLACK)
                                                setEntryLabelTextSize(11f)
                                                legend.isEnabled = true
                                                holeRadius = 42f
                                                transparentCircleRadius = 48f
                                            }
                                        },
                                        update = { chart ->
                                            val entries = categoryTimeMap.map { (cat, hours) ->
                                                PieEntry(hours, cat)
                                            }
                                            val dataSet = PieDataSet(entries, "").apply {
                                                colors = ColorTemplate.MATERIAL_COLORS.toList()
                                                valueTextColor = AndroidColor.BLACK
                                                valueTextSize = 11f
                                            }
                                            val pieData = PieData(dataSet).apply {
                                                setValueFormatter(PercentFormatter(chart))
                                                setValueTextSize(11f)
                                                setValueTextColor(AndroidColor.BLACK)
                                            }
                                            chart.data = pieData
                                            chart.invalidate()
                                        }
                                    )
                                }
                            }
                        }

                        // Category Breakdown header
                        item {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Breakdown Items
                        items(categoryTimeMap.toList()) { (category, hours) ->
                            val percentage = if (totalHours > 0f) (hours / totalHours * 100).toInt() else 0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = String.format("%.1f hours", hours),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AceRedLight.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "$percentage%",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AceRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
