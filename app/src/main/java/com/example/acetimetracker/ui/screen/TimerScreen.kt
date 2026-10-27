package com.example.acetimetracker.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.ui.theme.SuccessGreenContainer
import com.example.acetimetracker.ui.theme.WarningYellowContainer
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    categoriesState: Resource<List<Category>>?,
    elapsedTime: Long,
    isRunning: Boolean,
    onBackClick: () -> Unit,
    onToggleStartStop: () -> Unit,
    onResetTimer: () -> Unit,
    onSaveGoalsAndTime: (category: String, minGoal: Float, maxGoal: Float) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    var minGoalText by remember { mutableStateOf("1.0") }
    var maxGoalText by remember { mutableStateOf("4.0") }

    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()

    val minGoal = minGoalText.toFloatOrNull()
    val maxGoal = maxGoalText.toFloatOrNull()
    val currentHours = elapsedTime / (1000f * 60f * 60f)

    val feedbackMessage: String
    val isSuccessFeedback: Boolean

    if (minGoal != null && maxGoal != null && elapsedTime > 0L) {
        when {
            currentHours >= maxGoal -> {
                feedbackMessage = "🎉 You've reached your maximum goal! Great work!"
                isSuccessFeedback = true
            }
            currentHours >= minGoal -> {
                feedbackMessage = "✅ You're on track! Keep going!"
                isSuccessFeedback = true
            }
            else -> {
                feedbackMessage = "⏳ Behind minimum goal. Keep pushing!"
                isSuccessFeedback = false
            }
        }
    } else {
        feedbackMessage = "Set your goals and start tracking to receive live progress feedback."
        isSuccessFeedback = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productivity Timer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Category selector
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedCategory.isNotEmpty()) selectedCategory else "Select Category to Track",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
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
                            text = { Text(cat.name) },
                            onClick = {
                                selectedCategory = cat.name
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Stopwatch Hero Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = TimeUtils.formatDuration(elapsedTime),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isRunning) "● ACTIVE SESSION" else "PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timer Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = onResetTimer,
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play / Pause FAB
                FloatingActionButton(
                    onClick = onToggleStartStop,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Goals Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Session Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = minGoalText,
                            onValueChange = { minGoalText = it },
                            label = { Text("Min Goal (h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = maxGoalText,
                            onValueChange = { maxGoalText = it },
                            label = { Text("Max Goal (h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Live Goal Feedback Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccessFeedback) SuccessGreenContainer else WarningYellowContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackMessage,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Save Session Button
            Button(
                onClick = {
                    if (minGoal != null && maxGoal != null) {
                        onSaveGoalsAndTime(selectedCategory, minGoal, maxGoal)
                    }
                },
                enabled = minGoal != null && maxGoal != null && minGoal <= maxGoal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Session to Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
