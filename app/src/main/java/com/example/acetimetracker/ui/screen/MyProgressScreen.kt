package com.example.acetimetracker.ui.screen

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProgressScreen(
    categoriesState: Resource<List<Category>>?,
    timesheetsState: Resource<List<TimesheetEntry>>?,
    onBackClick: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()
    val allTimesheets = (timesheetsState as? Resource.Success)?.data ?: emptyList()

    val filteredTimesheets = remember(selectedCategory, allTimesheets) {
        if (selectedCategory == "All Categories") {
            allTimesheets
        } else {
            allTimesheets.filter { it.category == selectedCategory }
        }
    }

    val totalMillis = remember(filteredTimesheets) {
        filteredTimesheets.sumOf { it.timeSpent }
    }

    val targetCategory = categories.find { it.name == selectedCategory }
    val goalHours = targetCategory?.goal ?: 0f
    val currentSpentHours = totalMillis / (1000f * 60f * 60f)

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, Slate200)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Goal Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate800)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Filter Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories", fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedCategory = "All Categories"
                                categoryExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Total Allocated Time Banner in Ace Red
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AceRed
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Total Allocated Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = TimeUtils.formatDuration(totalMillis),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Comparison Chart (when a specific category is selected)
            if (selectedCategory != "All Categories") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Progress vs Target Goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                factory = { ctx ->
                                    BarChart(ctx).apply {
                                        description.isEnabled = false
                                        legend.isEnabled = false
                                        axisRight.isEnabled = false
                                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                                        xAxis.setDrawGridLines(false)
                                        xAxis.granularity = 1f
                                        setFitBars(true)
                                    }
                                },
                                update = { chart ->
                                    val entries = listOf(
                                        BarEntry(0f, currentSpentHours),
                                        BarEntry(1f, goalHours)
                                    )
                                    val dataSet = BarDataSet(entries, "Hours").apply {
                                        colors = listOf(
                                            AndroidColor.parseColor("#D32F2F"), // Ace Red
                                            AndroidColor.parseColor("#388E3C")  // Dark Green
                                        )
                                        valueTextColor = AndroidColor.BLACK
                                        valueTextSize = 12f
                                    }
                                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Current", "Goal"))
                                    chart.data = BarData(dataSet)
                                    chart.invalidate()
                                }
                            )
                        }
                    }
                }
            }

            // Timesheet entries header
            item {
                Text(
                    text = "Timesheet Log (${filteredTimesheets.size} entries)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Timesheet List
            items(filteredTimesheets) { entry ->
                TimesheetEntryCard(entry = entry)
            }
        }
    }
}

@Composable
fun TimesheetEntryCard(entry: TimesheetEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AceRed
                )
                Text(
                    text = TimeUtils.formatDuration(entry.timeSpent),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${entry.startDate} ${entry.startTime} → ${entry.endDate} ${entry.endTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (entry.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (entry.imageUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = "Entry Proof",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
