package com.example.acetimetracker.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimesheetScreen(
    categoriesState: Resource<List<Category>>?,
    saveEntryState: Resource<*>?,
    uploadImageState: Resource<*>?,
    selectedImageUri: Uri?,
    onBackClick: () -> Unit,
    onSelectImageClick: () -> Unit,
    onSaveClick: (category: String, startDate: String, startTime: String, endDate: String, endTime: String, description: String) -> Unit
) {
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val isSaving = saveEntryState is Resource.Loading
    val isUploading = uploadImageState is Resource.Loading

    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()

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
                TopAppBar(
                    title = {
                        Text(
                            text = "Log Timesheet Entry",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Dropdown
            Text(
                text = "Category *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = if (selectedCategory.isNotEmpty()) selectedCategory else "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = AceRed) },
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
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedCategory = cat.name
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Start Date & Time
            Text(
                text = "Start Interval *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AceBlue) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker { startDate = it } },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = AceBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Time") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = AceBlue) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker { startTime = it } },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = AceBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // End Date & Time
            Text(
                text = "End Interval *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = endDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AceRed) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker { endDate = it } },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = AceRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Time") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = AceRed) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker { endTime = it } },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = AceRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Description
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What did you accomplish?") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Image Upload Section
            Text(
                text = "Photo Proof / Documentation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Picture",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedButton(
                        onClick = onSelectImageClick,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = AceRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedImageUri != null) "Change Photo" else "Attach Photo Proof", color = AceRed)
                    }

                    if (isUploading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AceRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading photo...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button in Ace Red
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
                enabled = selectedCategory.isNotBlank() &&
                        startDate.isNotBlank() &&
                        startTime.isNotBlank() &&
                        endDate.isNotBlank() &&
                        endTime.isNotBlank() &&
                        !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AceRed,
                    contentColor = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Timesheet Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
