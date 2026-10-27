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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.util.Locale

enum class TimerMode {
    STOPWATCH,
    POMODORO
}

enum class PomodoroPhase {
    FOCUS,
    BREAK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    categoriesState: Resource<List<Category>>?,
    mode: TimerMode,
    onModeChange: (TimerMode) -> Unit,
    // Stopwatch parameters
    stopwatchElapsedTime: Long,
    isStopwatchRunning: Boolean,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onSaveGoalsAndTime: (category: String, minGoal: Float, maxGoal: Float) -> Unit,
    // Pomodoro parameters
    pomodoroPhase: PomodoroPhase,
    pomodoroRemainingTime: Long,
    pomodoroTotalPhaseDuration: Long,
    isPomodoroRunning: Boolean,
    pomodoroRoundsCompleted: Int,
    onTogglePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSkipPomodoroPhase: () -> Unit,
    onSelectPomodoroPreset: (focusMinutes: Int, breakMinutes: Int) -> Unit,
    onSavePomodoroSession: (category: String) -> Unit,
    // Common
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var minGoalText by remember { mutableStateOf("1.0") }
    var maxGoalText by remember { mutableStateOf("4.0") }

    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, Slate200)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (mode == TimerMode.STOPWATCH) "Stopwatch Timer" else "Pomodoro Focus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate800)
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
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = if (mode == TimerMode.STOPWATCH) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AceRed
            ) {
                Tab(
                    selected = mode == TimerMode.STOPWATCH,
                    onClick = { onModeChange(TimerMode.STOPWATCH) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stopwatch", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = mode == TimerMode.POMODORO,
                    onClick = { onModeChange(TimerMode.POMODORO) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pomodoro Focus", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Category Selector (Shared)
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
                                    onSelectCategory(cat.name)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                if (mode == TimerMode.STOPWATCH) {
                    // STOPWATCH CONTENT
                    StopwatchView(
                        elapsedTime = stopwatchElapsedTime,
                        isRunning = isStopwatchRunning,
                        minGoalText = minGoalText,
                        maxGoalText = maxGoalText,
                        onMinGoalChange = { minGoalText = it },
                        onMaxGoalChange = { maxGoalText = it },
                        onToggleStartStop = onToggleStopwatch,
                        onReset = onResetStopwatch,
                        onSaveSession = {
                            val min = minGoalText.toFloatOrNull()
                            val max = maxGoalText.toFloatOrNull()
                            if (min != null && max != null) {
                                onSaveGoalsAndTime(selectedCategory, min, max)
                            }
                        }
                    )
                } else {
                    // POMODORO CONTENT
                    PomodoroView(
                        phase = pomodoroPhase,
                        remainingTime = pomodoroRemainingTime,
                        totalPhaseDuration = pomodoroTotalPhaseDuration,
                        isRunning = isPomodoroRunning,
                        roundsCompleted = pomodoroRoundsCompleted,
                        onToggleStartStop = onTogglePomodoro,
                        onReset = onResetPomodoro,
                        onSkipPhase = onSkipPomodoroPhase,
                        onSelectPreset = onSelectPomodoroPreset,
                        onSaveSession = { onSavePomodoroSession(selectedCategory) }
                    )
                }
            }
        }
    }
}

@Composable
fun StopwatchView(
    elapsedTime: Long,
    isRunning: Boolean,
    minGoalText: String,
    maxGoalText: String,
    onMinGoalChange: (String) -> Unit,
    onMaxGoalChange: (String) -> Unit,
    onToggleStartStop: () -> Unit,
    onReset: () -> Unit,
    onSaveSession: () -> Unit
) {
    val minGoal = minGoalText.toFloatOrNull()
    val maxGoal = maxGoalText.toFloatOrNull()
    val currentHours = elapsedTime / (1000f * 60f * 60f)

    val feedbackMessage: String
    val feedbackBgColor: Color
    val feedbackTextColor: Color

    if (minGoal != null && maxGoal != null && elapsedTime > 0L) {
        when {
            currentHours >= maxGoal -> {
                feedbackMessage = "🎉 Maximum goal reached! Outstanding work!"
                feedbackBgColor = AceLightGreen
                feedbackTextColor = AceDarkGreen
            }
            currentHours >= minGoal -> {
                feedbackMessage = "✅ On track with your goal! Keep going!"
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

    // Hero Stopwatch Display
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) AceRedLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

    // Controls
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onReset,
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

        FloatingActionButton(
            onClick = onToggleStartStop,
            modifier = Modifier.size(74.dp),
            shape = CircleShape,
            containerColor = if (isRunning) Color(0xFFC62828) else AceRed,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
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
        border = BorderStroke(1.dp, Slate200),
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
                    onValueChange = onMinGoalChange,
                    label = { Text("Min Goal (h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = maxGoalText,
                    onValueChange = onMaxGoalChange,
                    label = { Text("Max Goal (h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

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

    Button(
        onClick = onSaveSession,
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
        Text("Save Stopwatch Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PomodoroView(
    phase: PomodoroPhase,
    remainingTime: Long,
    totalPhaseDuration: Long,
    isRunning: Boolean,
    roundsCompleted: Int,
    onToggleStartStop: () -> Unit,
    onReset: () -> Unit,
    onSkipPhase: () -> Unit,
    onSelectPreset: (focusMinutes: Int, breakMinutes: Int) -> Unit,
    onSaveSession: () -> Unit
) {
    val isFocus = phase == PomodoroPhase.FOCUS
    val progress = if (totalPhaseDuration > 0L) {
        ((totalPhaseDuration - remainingTime).toFloat() / totalPhaseDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val totalSeconds = (remainingTime / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    // Presets Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onSelectPreset(25, 5) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (totalPhaseDuration == 25 * 60 * 1000L || totalPhaseDuration == 5 * 60 * 1000L) AceRed else Slate700
            )
        ) {
            Text("25 / 5m Standard", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = { onSelectPreset(50, 10) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (totalPhaseDuration == 50 * 60 * 1000L || totalPhaseDuration == 10 * 60 * 1000L) AceRed else Slate700
            )
        ) {
            Text("50 / 10m Deep Work", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }

    // Pomodoro Hero Display
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocus) {
                if (isRunning) AceRedLight.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
            } else {
                AceLightGreen.copy(alpha = 0.5f)
            }
        ),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Phase indicator badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isFocus) AceRed.copy(alpha = 0.15f) else AceDarkGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isFocus) "🔥 FOCUS ROUND" else "☕ REST BREAK",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocus) AceRed else AceDarkGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 54.sp
                ),
                color = if (isFocus) AceRed else AceDarkGreen
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isFocus) AceRed else AceDarkGreen,
                trackColor = Slate200
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Rounds Completed: $roundsCompleted",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Slate600
            )
        }
    }

    // Controls
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Pomodoro",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Play / Pause Hero Button
        FloatingActionButton(
            onClick = onToggleStartStop,
            modifier = Modifier.size(74.dp),
            shape = CircleShape,
            containerColor = if (isFocus) (if (isRunning) Color(0xFFC62828) else AceRed) else AceDarkGreen,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Skip Phase Button
        IconButton(
            onClick = onSkipPhase,
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip Phase",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Button(
        onClick = onSaveSession,
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
        Text("Save Focus Session to Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
