package com.example.acetimetracker.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterPeriod(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimesheetScreen(
    categoriesState: Resource<List<Category>>?,
    timesheetsState: Resource<List<TimesheetEntry>>? = null,
    saveEntryState: Resource<*>?,
    uploadImageState: Resource<*>?,
    selectedImageUri: Uri?,
    initialTab: Int = 0,
    onBackClick: () -> Unit,
    onSelectImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit = {},
    onSaveClick: (category: String, startDate: String, startTime: String, endDate: String, endTime: String, description: String) -> Unit,
    onDeleteEntryClick: (String) -> Unit = {},
    onExportCsvClick: (List<TimesheetEntry>) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }

    // Form state
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // History filter state
    var selectedCategoryFilter by remember { mutableStateOf("All Categories") }
    var categoryFilterExpanded by remember { mutableStateOf(false) }
    var selectedDatePeriod by remember { mutableStateOf(DateFilterPeriod.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var entryToDelete by remember { mutableStateOf<TimesheetEntry?>(null) }

    val isSaving = saveEntryState is Resource.Loading
    val isUploading = uploadImageState is Resource.Loading

    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()
    val allTimesheets = (timesheetsState as? Resource.Success)?.data ?: emptyList()

    // Calculated time spent preview in Form
    val calculatedDuration = remember(startTime, endTime) {
        if (startTime.isNotBlank() && endTime.isNotBlank()) {
            TimeUtils.calculateTimeSpentMillis(startTime, endTime)
        } else 0L
    }

    // Filtered timesheets in History
    val filteredTimesheets = remember(allTimesheets, selectedCategoryFilter, selectedDatePeriod, searchQuery) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startOfWeekStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        allTimesheets.filter { entry ->
            val matchesCategory = selectedCategoryFilter == "All Categories" || entry.category == selectedCategoryFilter
            val matchesDate = when (selectedDatePeriod) {
                DateFilterPeriod.ALL -> true
                DateFilterPeriod.TODAY -> entry.startDate == todayStr
                DateFilterPeriod.THIS_WEEK -> entry.startDate >= startOfWeekStr
                DateFilterPeriod.THIS_MONTH -> entry.startDate.startsWith(currentMonthPrefix)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    entry.category.contains(searchQuery, ignoreCase = true) ||
                    entry.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesDate && matchesSearch
        }
    }

    val totalFilteredMillis = remember(filteredTimesheets) {
        filteredTimesheets.sumOf { it.timeSpent }
    }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onDateSelected(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, Slate200)
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Timesheet Management",
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
                        actions = {
                            if (selectedTabIndex == 1 && filteredTimesheets.isNotEmpty()) {
                                IconButton(onClick = { onExportCsvClick(filteredTimesheets) }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Export & Share Report",
                                        tint = AceRed
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )

                    // Clean Segmented Tab Row
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = AceRed,
                        divider = { HorizontalDivider(color = Slate200) }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    text = "Log New Entry",
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    text = "Entries & History (${allTimesheets.size})",
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Slate50)
        ) {
            if (selectedTabIndex == 0) {
                // TAB 0: LOG NEW ENTRY FORM
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Task Category *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = !categoryExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategory.ifEmpty { "Select a category" },
                                    onValueChange = {},
                                    readOnly = true,
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
                                    if (categories.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No categories found. Create one first!") },
                                            onClick = { categoryExpanded = false }
                                        )
                                    } else {
                                        categories.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category.name) },
                                                onClick = {
                                                    selectedCategory = category.name
                                                    categoryExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Date & Time Selectors
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Date & Time Boundaries *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )

                            // Start Date & Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = startDate,
                                    onValueChange = { startDate = it },
                                    label = { Text("Start Date") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePicker { startDate = it } }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Pick Start Date", tint = AceRed)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    readOnly = true
                                )

                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text("Start Time") },
                                    placeholder = { Text("HH:MM") },
                                    trailingIcon = {
                                        IconButton(onClick = { showTimePicker { startTime = it } }) {
                                            Icon(Icons.Default.Schedule, contentDescription = "Pick Start Time", tint = AceRed)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    readOnly = true
                                )
                            }

                            // End Date & Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = endDate,
                                    onValueChange = { endDate = it },
                                    label = { Text("End Date") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePicker { endDate = it } }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Pick End Date", tint = AceRed)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    readOnly = true
                                )

                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    label = { Text("End Time") },
                                    placeholder = { Text("HH:MM") },
                                    trailingIcon = {
                                        IconButton(onClick = { showTimePicker { endTime = it } }) {
                                            Icon(Icons.Default.Schedule, contentDescription = "Pick End Time", tint = AceRed)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    readOnly = true
                                )
                            }

                            // Live Calculated Duration Banner
                            if (calculatedDuration > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentEmeraldLight
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Calculated Study Duration:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = AceDarkGreen
                                        )
                                        Text(
                                            text = TimeUtils.formatDuration(calculatedDuration),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AceDarkGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notes / Description Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Study Notes & Description",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("What did you accomplish during this session?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4
                            )
                        }
                    }

                    // Attached Photo Evidence Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Photo Proof / Notes Evidence",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                                if (selectedImageUri != null) {
                                    TextButton(onClick = onRemoveImageClick) {
                                        Text("Remove", color = AceRed, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (selectedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { fullScreenImageUrl = selectedImageUri.toString() }
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Tap to enlarge",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onSelectImageClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Slate300)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Slate700)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isUploading) "Uploading Photo..." else "Attach Study Proof / Image",
                                        color = Slate800,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            onSaveClick(
                                selectedCategory,
                                startDate,
                                startTime,
                                endDate,
                                endTime,
                                description
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AceRed),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Timesheet Entry", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // TAB 1: ENTRIES & HISTORY LIST
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filter Controls Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by category or notes...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            // Date Period Filter Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DateFilterPeriod.entries.forEach { period ->
                                    val isSelected = selectedDatePeriod == period
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) AceRed else Slate100,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedDatePeriod = period }
                                    ) {
                                        Text(
                                            text = period.label,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Slate700,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Category Filter Dropdown
                            ExposedDropdownMenuBox(
                                expanded = categoryFilterExpanded,
                                onExpandedChange = { categoryFilterExpanded = !categoryFilterExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategoryFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryFilterExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = categoryFilterExpanded,
                                    onDismissRequest = { categoryFilterExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Categories", fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedCategoryFilter = "All Categories"
                                            categoryFilterExpanded = false
                                        }
                                    )
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                selectedCategoryFilter = cat.name
                                                categoryFilterExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Total Summary Strip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "FILTERED TOTAL",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    color = Slate400,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = TimeUtils.formatDuration(totalFilteredMillis),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate800,
                                border = BorderStroke(1.dp, Slate700)
                            ) {
                                Text(
                                    text = "${filteredTimesheets.size} Entries",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate200
                                )
                            }
                        }
                    }

                    // Entries List
                    if (filteredTimesheets.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No timesheet entries found",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try adjusting your filters or log a new study session in the first tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredTimesheets, key = { it.id }) { entry ->
                                TimesheetHistoryCard(
                                    entry = entry,
                                    onImageClick = { fullScreenImageUrl = entry.imageUrl },
                                    onDeleteClick = { entryToDelete = entry }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Image Dialog
        if (fullScreenImageUrl != null) {
            Dialog(onDismissRequest = { fullScreenImageUrl = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = fullScreenImageUrl,
                            contentDescription = "Full Size Evidence",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        IconButton(
                            onClick = { fullScreenImageUrl = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (entryToDelete != null) {
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("Delete Entry?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Are you sure you want to delete this ${entryToDelete?.category} session from ${entryToDelete?.startDate}?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            entryToDelete?.id?.let { onDeleteEntryClick(it) }
                            entryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AceRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TimesheetHistoryCard(
    entry: TimesheetEntry,
    onImageClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge, Duration, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AceRedLight
                ) {
                    Text(
                        text = entry.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AceRed
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeUtils.formatDuration(entry.timeSpent),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Time Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${entry.startDate} • ${entry.startTime} - ${entry.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }

            // Description / Note
            if (entry.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                    lineHeight = 18.sp
                )
            }

            // Photo Evidence Preview
            if (entry.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onImageClick)
                        .background(Slate100)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Photo Evidence",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                }
            }
        }
    }
}
