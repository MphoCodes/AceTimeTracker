package com.example.acetimetracker.ui.screen

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
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
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
import com.example.acetimetracker.ui.components.AceDatePickerDialog
import com.example.acetimetracker.ui.components.AceTimePickerDialog
import com.example.acetimetracker.ui.components.AceAlertDialog
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class PickerTarget { StartDate, StartTime, EndDate, EndTime }

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
    formResetKey: Int = 0,
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

    // Form state, cleared whenever formResetKey changes after a successful save
    var selectedCategory by remember(formResetKey) { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var startDate by remember(formResetKey) { mutableStateOf(TimeUtils.today()) }
    var startTime by remember(formResetKey) { mutableStateOf("") }
    var endDate by remember(formResetKey) { mutableStateOf(TimeUtils.today()) }
    var endTime by remember(formResetKey) { mutableStateOf("") }
    var description by remember(formResetKey) { mutableStateOf("") }

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
    val calculatedDuration = remember(startDate, startTime, endDate, endTime) {
        TimeUtils.calculateTimeSpentMillis(startDate, startTime, endDate, endTime)
    }
    val hasCompleteRange = startDate.isNotBlank() && startTime.isNotBlank() && endDate.isNotBlank() && endTime.isNotBlank()
    val canSave = selectedCategory.isNotBlank() && hasCompleteRange && calculatedDuration > 0L && !isSaving && !isUploading

    // Filtered timesheets in History
    val filteredTimesheets = remember(allTimesheets, selectedCategoryFilter, selectedDatePeriod, searchQuery) {
        val todayStr = TimeUtils.today()
        val startOfWeekStr = TimeUtils.startOfWeek()
        val currentMonthPrefix = todayStr.take(7)

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

    var activePicker by remember { mutableStateOf<PickerTarget?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Timesheet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                            }
                        },
                        actions = {
                            if (selectedTabIndex == 1 && filteredTimesheets.isNotEmpty()) {
                                IconButton(onClick = { onExportCsvClick(filteredTimesheets) }) {
                                    Icon(
                                        Lucide.Share2,
                                        contentDescription = "Export & Share Report",
                                        tint = AceRed,
                                        modifier = Modifier.size(20.dp)
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
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    text = "Log entry",
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
                                    text = "History (${allTimesheets.size})",
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
                .background(MaterialTheme.colorScheme.background)
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Category *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "When *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Start Date & Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PickerField(
                                    value = startDate,
                                    label = "Start date",
                                    icon = Lucide.Calendar,
                                    onClick = { activePicker = PickerTarget.StartDate },
                                    modifier = Modifier.weight(1f)
                                )
                                PickerField(
                                    value = startTime,
                                    label = "Start time",
                                    icon = Lucide.Clock,
                                    onClick = { activePicker = PickerTarget.StartTime },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // End Date & Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PickerField(
                                    value = endDate,
                                    label = "End date",
                                    icon = Lucide.Calendar,
                                    onClick = { activePicker = PickerTarget.EndDate },
                                    modifier = Modifier.weight(1f)
                                )
                                PickerField(
                                    value = endTime,
                                    label = "End time",
                                    icon = Lucide.Clock,
                                    onClick = { activePicker = PickerTarget.EndTime },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Live Calculated Duration Banner
                            if (hasCompleteRange && calculatedDuration <= 0L) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "End must be after start. Check the dates and times.",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            if (calculatedDuration > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Duration",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = TimeUtils.formatDurationShort(calculatedDuration),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Photo (optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Lucide.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isUploading) "Uploading photo…" else "Attach a photo",
                                        color = MaterialTheme.colorScheme.onSurface,
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
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AceRed, contentColor = Color.White),
                        enabled = canSave
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Lucide.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isUploading) "Waiting for photo…" else "Save entry", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by category or notes...") },
                                leadingIcon = { Icon(Lucide.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Lucide.X, contentDescription = "Clear", modifier = Modifier.size(16.dp))
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
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDatePeriod = period },
                                        label = {
                                            Text(
                                                text = period.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AceRed,
                                            selectedLabelColor = Color.White
                                        )
                                    )
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
                        color = MaterialTheme.colorScheme.primaryContainer
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
                                    text = "TOTAL",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = TimeUtils.formatDurationShort(totalFilteredMillis),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = if (filteredTimesheets.size == 1) "1 entry" else "${filteredTimesheets.size} entries",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Entries List
                    if (filteredTimesheets.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Lucide.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No timesheet entries found",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try adjusting your filters, or log a new session from the first tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        when (activePicker) {
            PickerTarget.StartDate -> AceDatePickerDialog(
                initialDate = startDate,
                onDismiss = { activePicker = null },
                onConfirm = {
                    startDate = it
                    if (endDate < it) endDate = it
                    activePicker = null
                }
            )
            PickerTarget.EndDate -> AceDatePickerDialog(
                initialDate = endDate,
                onDismiss = { activePicker = null },
                onConfirm = {
                    endDate = it
                    activePicker = null
                }
            )
            PickerTarget.StartTime -> AceTimePickerDialog(
                initialTime = startTime,
                onDismiss = { activePicker = null },
                onConfirm = {
                    startTime = it
                    activePicker = null
                }
            )
            PickerTarget.EndTime -> AceTimePickerDialog(
                initialTime = endTime,
                onDismiss = { activePicker = null },
                onConfirm = {
                    endTime = it
                    activePicker = null
                }
            )
            null -> Unit
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
                            Icon(Lucide.X, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (entryToDelete != null) {
            AceAlertDialog(
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

/** Read-only field that opens a picker when tapped anywhere, not just on its icon. */
@Composable
private fun PickerField(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("Tap to pick") },
            trailingIcon = { Icon(icon, contentDescription = null, tint = AceRed, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            readOnly = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "Pick $label", onClick = onClick)
        )
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = entry.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeUtils.formatDuration(entry.timeSpent),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDeleteClick) {
                        Icon(Lucide.Trash2, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Time Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.Clock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (entry.endDate.isNotBlank() && entry.endDate != entry.startDate) {
                        "${entry.startDate} ${entry.startTime} → ${entry.endDate} ${entry.endTime}"
                    } else {
                        "${entry.startDate} • ${entry.startTime} – ${entry.endTime}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description / Note
            if (entry.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View photo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
