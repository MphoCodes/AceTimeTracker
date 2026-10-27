package com.example.acetimetracker.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.ui.components.AceCard
import com.example.acetimetracker.ui.components.AceTopBar
import com.example.acetimetracker.ui.components.SectionLabel
import com.example.acetimetracker.ui.components.AceAlertDialog
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
    // Stopwatch
    stopwatchElapsedTime: Long,
    isStopwatchRunning: Boolean,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    // Pomodoro
    pomodoroPhase: PomodoroPhase,
    pomodoroRemainingTime: Long,
    pomodoroTotalPhaseDuration: Long,
    pomodoroFocusMinutes: Int,
    isPomodoroRunning: Boolean,
    pomodoroRoundsCompleted: Int,
    pomodoroFocusLogged: Long,
    onTogglePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSkipPomodoroPhase: () -> Unit,
    onSelectPomodoroPreset: (focusMinutes: Int, breakMinutes: Int) -> Unit,
    // Common
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    isSaving: Boolean,
    onSaveSession: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
    onCreateCategoryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()
    val snackbarHostState = remember { SnackbarHostState() }
    val isRunning = isStopwatchRunning || isPomodoroRunning
    val unsaved = if (mode == TimerMode.STOPWATCH) stopwatchElapsedTime else pomodoroFocusLogged
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    // Leaving discards in-progress time, so confirm first.
    BackHandler(enabled = isRunning || unsaved >= 1000L) { showLeaveDialog = true }
    val requestBack = { if (isRunning || unsaved >= 1000L) showLeaveDialog = true else onBackClick() }

    Scaffold(
        topBar = { AceTopBar(title = "Timer", onBackClick = requestBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == TimerMode.STOPWATCH,
                    onClick = { onModeChange(TimerMode.STOPWATCH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(PhosphorIcons.Regular.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = segmentedColors()
                ) { Text("Stopwatch") }
                SegmentedButton(
                    selected = mode == TimerMode.POMODORO,
                    onClick = { onModeChange(TimerMode.POMODORO) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(PhosphorIcons.Regular.Hourglass, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = segmentedColors()
                ) { Text("Focus") }
            }

            CategoryPicker(
                categories = categories,
                isLoading = categoriesState is Resource.Loading,
                selected = selectedCategory,
                onSelect = onSelectCategory,
                onCreateCategoryClick = onCreateCategoryClick
            )

            val selectedGoalHours = categories.find { it.name == selectedCategory }?.goal

            if (mode == TimerMode.STOPWATCH) {
                StopwatchView(
                    elapsedTime = stopwatchElapsedTime,
                    isRunning = isStopwatchRunning,
                    goalHours = selectedGoalHours,
                    onToggleStartStop = onToggleStopwatch,
                    onReset = onResetStopwatch
                )
            } else {
                PomodoroView(
                    phase = pomodoroPhase,
                    remainingTime = pomodoroRemainingTime,
                    totalPhaseDuration = pomodoroTotalPhaseDuration,
                    focusMinutes = pomodoroFocusMinutes,
                    isRunning = isPomodoroRunning,
                    roundsCompleted = pomodoroRoundsCompleted,
                    focusLogged = pomodoroFocusLogged,
                    onToggleStartStop = onTogglePomodoro,
                    onReset = onResetPomodoro,
                    onSkipPhase = onSkipPomodoroPhase,
                    onSelectPreset = onSelectPomodoroPreset
                )
            }

            Button(
                onClick = onSaveSession,
                enabled = !isSaving && unsaved >= 1000L && selectedCategory.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AceRed, contentColor = Color.White)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(PhosphorIcons.Regular.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            selectedCategory.isBlank() -> "Pick a category to save"
                            unsaved < 1000L -> "Save session"
                            else -> "Save ${TimeUtils.formatDurationShort(unsaved).let { if (it == "0m") "<1m" else it }} to $selectedCategory"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }
        }
    }

    if (showLeaveDialog) {
        AceAlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave without saving?") },
            text = { Text("You have ${TimeUtils.formatDurationShort(unsaved).let { if (it == "0m") "less than a minute" else it }} that hasn't been saved. Leaving will discard it.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onBackClick()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Keep timing") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun segmentedColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    categories: List<Category>,
    isLoading: Boolean,
    selected: String,
    onSelect: (String) -> Unit,
    onCreateCategoryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("What are you working on?")
        when {
            isLoading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AceRed)
            categories.isEmpty() -> AceCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Create a category so your tracked time has somewhere to go.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = onCreateCategoryClick) {
                    Icon(PhosphorIcons.Regular.FolderPlus, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create category")
                }
            }
            else -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val color = parseHexColor(cat.colorHex)
                    val isSelected = cat.name == selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(cat.name) },
                        label = { Text(cat.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = color,
                            selectedBorderWidth = 1.5.dp
                        ),
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                }
            }
        }
    }
}

/** Circular progress ring with arbitrary centered content. */
@Composable
private fun TimerRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    resetLabel: String,
    trailing: (@Composable () -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(onClick = onReset, modifier = Modifier.size(56.dp)) {
            Icon(PhosphorIcons.Regular.ArrowCounterClockwise, contentDescription = resetLabel, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(28.dp))
        LargeFloatingActionButton(
            onClick = onToggle,
            shape = CircleShape,
            containerColor = accent,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isRunning) PhosphorIcons.Regular.Pause else PhosphorIcons.Regular.Play,
                contentDescription = if (isRunning) "Pause" else "Start",
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.width(28.dp))
        if (trailing != null) trailing() else Spacer(modifier = Modifier.size(56.dp))
    }
}

@Composable
fun StopwatchView(
    elapsedTime: Long,
    isRunning: Boolean,
    goalHours: Float?,
    onToggleStartStop: () -> Unit,
    onReset: () -> Unit
) {
    val goalMs = goalHours?.takeIf { it > 0f }?.let { (it * 60 * 60 * 1000).toLong() }
    val progress = if (goalMs != null) elapsedTime.toFloat() / goalMs else if (elapsedTime > 0) 1f else 0f
    val ringColor = if (goalMs != null && elapsedTime >= goalMs) AccentEmerald else AceRed
    val timeText = TimeUtils.formatDuration(elapsedTime)

    TimerRing(
        progress = progress,
        color = ringColor,
        modifier = Modifier.semantics { contentDescription = "Elapsed $timeText" }
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 40.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                goalMs == null -> if (isRunning) "Tracking…" else if (elapsedTime > 0) "Paused" else "Ready"
                elapsedTime >= goalMs -> "Goal reached"
                else -> "of ${formatHours(goalHours)} goal"
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (goalMs != null && elapsedTime >= goalMs) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }

    TimerControls(
        isRunning = isRunning,
        accent = AceRed,
        onToggle = onToggleStartStop,
        onReset = onReset,
        resetLabel = "Reset stopwatch",
        trailing = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroView(
    phase: PomodoroPhase,
    remainingTime: Long,
    totalPhaseDuration: Long,
    focusMinutes: Int,
    isRunning: Boolean,
    roundsCompleted: Int,
    focusLogged: Long,
    onToggleStartStop: () -> Unit,
    onReset: () -> Unit,
    onSkipPhase: () -> Unit,
    onSelectPreset: (focusMinutes: Int, breakMinutes: Int) -> Unit
) {
    val isFocus = phase == PomodoroPhase.FOCUS
    val accent = if (isFocus) AceRed else AccentEmerald
    val progress = if (totalPhaseDuration > 0L) 1f - remainingTime.toFloat() / totalPhaseDuration else 0f
    val totalSeconds = (remainingTime / 1000).coerceAtLeast(0)
    val timeFormatted = String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(25 to 5, 50 to 10).forEach { (focus, rest) ->
            FilterChip(
                selected = focusMinutes == focus,
                onClick = { onSelectPreset(focus, rest) },
                label = {
                    Text(
                        "$focus min focus · $rest break",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }

    TimerRing(
        progress = progress,
        color = accent,
        modifier = Modifier.semantics { contentDescription = "${if (isFocus) "Focus" else "Break"}, $timeFormatted remaining" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isFocus) PhosphorIcons.Regular.Flame else PhosphorIcons.Regular.Coffee,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isFocus) "FOCUS" else "BREAK",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = timeFormatted,
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Round ${roundsCompleted + 1} · ${TimeUtils.formatDurationShort(focusLogged)} focused",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    TimerControls(
        isRunning = isRunning,
        accent = accent,
        onToggle = onToggleStartStop,
        onReset = onReset,
        resetLabel = "Reset phase",
        trailing = {
            FilledTonalIconButton(onClick = onSkipPhase, modifier = Modifier.size(56.dp)) {
                Icon(
                    PhosphorIcons.Regular.SkipForward,
                    contentDescription = if (isFocus) "Skip to break" else "Skip to focus",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}
