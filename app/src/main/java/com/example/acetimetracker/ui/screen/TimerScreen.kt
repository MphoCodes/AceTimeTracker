package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.ui.theme.*
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
    val feedbackBgColor: Color
    val feedbackTextColor: Color

    if (minGoal != null && maxGoal != null && elapsedTime > 0L) {
        when {
            currentHours >= maxGoal -> {
                feedbackMessage = "🎉 You've reached your maximum goal! Great work!"
                feedbackBgColor = AceLightGreen
                feedbackTextColor = AceDarkGreen
            }
            currentHours >= minGoal -> {
                feedbackMessage = "✅ You're on track! Keep going!"
                feedbackBgColor = AceLightGreen
                feedbackTextColor = AceDarkGreen
            }
            else -> {
                feedbackMessage = "⏳ Behind minimum goal. Keep pushing!"
                feedbackBgColor = AceLightYellow
                feedbackTextColor = Color(0xFF7A5800)
            }
        }
    } else {
        feedbackMessage = "Set your goals and start tracking to receive live progress feedback."
        feedbackBgColor = AceLightBlue
        feedbackTextColor = AceDarkBlue
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
                            text = "Stopwatch Timer",
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
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = AceOrange) },
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

            // Stopwatch Hero Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) {
                        AceRedLight.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = TimeUtils.formatDuration(elapsedTime),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp
                        ),
                        color = AceRed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isRunning) "● ACTIVE SESSION" else "PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) AceRed else MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.width(28.dp))

                // Play / Pause Hero Button
                FloatingActionButton(
                    onClick = onToggleStartStop,
                    modifier = Modifier.size(74.dp),
                    shape = CircleShape,
                    containerColor = if (isRunning) Color(0xFFC62828) else AceRed,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        modifier = Modifier.size(38.dp)
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                    // Live Goal Feedback Card with authentic vibrant colors
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = feedbackBgColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackMessage,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = feedbackTextColor
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
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AceRed,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Session to Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
