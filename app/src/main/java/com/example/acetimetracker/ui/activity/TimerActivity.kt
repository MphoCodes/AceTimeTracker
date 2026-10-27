package com.example.acetimetracker.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.UserRepository
import com.example.acetimetracker.service.TimerForegroundService
import com.example.acetimetracker.ui.screen.PomodoroPhase
import com.example.acetimetracker.ui.screen.TimerMode
import com.example.acetimetracker.ui.screen.TimerScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.CategoryViewModel
import com.example.acetimetracker.util.TimeUtils
import com.google.firebase.firestore.FirebaseFirestore

class TimerActivity : ComponentActivity() {

    private val categoryViewModel: CategoryViewModel by viewModels()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val handler = Handler(Looper.getMainLooper())

    // Active Mode & Category
    private var activeMode by mutableStateOf(TimerMode.STOPWATCH)
    private var selectedCategoryName by mutableStateOf("")

    // Stopwatch State
    private var isStopwatchRunning by mutableStateOf(false)
    private var stopwatchElapsedMs by mutableLongStateOf(0L)
    private var stopwatchStartTime = 0L

    // Pomodoro State
    private var isPomodoroRunning by mutableStateOf(false)
    private var pomodoroPhase by mutableStateOf(PomodoroPhase.FOCUS)
    private var pomodoroFocusMinutes by mutableIntStateOf(25)
    private var pomodoroBreakMinutes by mutableIntStateOf(5)
    private var pomodoroTotalDurationMs by mutableLongStateOf(25 * 60 * 1000L)
    private var pomodoroRemainingMs by mutableLongStateOf(25 * 60 * 1000L)
    private var pomodoroLastTickTime = 0L
    private var pomodoroRoundsCompleted by mutableIntStateOf(0)
    private var pomodoroAccumulatedFocusMs by mutableLongStateOf(0L)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    // Stopwatch Runnable
    private val stopwatchRunnable = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            stopwatchElapsedMs += now - stopwatchStartTime
            stopwatchStartTime = now

            TimerForegroundService.startOrUpdate(
                this@TimerActivity,
                selectedCategoryName,
                TimeUtils.formatDuration(stopwatchElapsedMs),
                "Stopwatch"
            )
            handler.postDelayed(this, 1000)
        }
    }

    // Pomodoro Runnable
    private val pomodoroRunnable = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val delta = now - pomodoroLastTickTime
            pomodoroLastTickTime = now

            if (pomodoroPhase == PomodoroPhase.FOCUS) {
                pomodoroAccumulatedFocusMs += delta
            }

            pomodoroRemainingMs = (pomodoroRemainingMs - delta).coerceAtLeast(0L)

            val totalSec = (pomodoroRemainingMs / 1000).coerceAtLeast(0)
            val min = totalSec / 60
            val sec = totalSec % 60
            val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", min, sec)
            val phaseLabel = if (pomodoroPhase == PomodoroPhase.FOCUS) "🍅 Focus" else "☕ Break"

            TimerForegroundService.startOrUpdate(
                this@TimerActivity,
                selectedCategoryName,
                formattedTime,
                phaseLabel
            )

            if (pomodoroRemainingMs <= 0L) {
                onPomodoroPhaseFinished()
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        setContent {
            AceTimeTrackerTheme {
                val categoriesState by categoryViewModel.categoriesState.observeAsState()

                TimerScreen(
                    categoriesState = categoriesState,
                    mode = activeMode,
                    onModeChange = { newMode ->
                        if (activeMode != newMode) {
                            if (isStopwatchRunning) stopStopwatch()
                            if (isPomodoroRunning) stopPomodoro()
                            activeMode = newMode
                        }
                    },
                    // Stopwatch
                    stopwatchElapsedTime = stopwatchElapsedMs,
                    isStopwatchRunning = isStopwatchRunning,
                    onToggleStopwatch = {
                        if (isStopwatchRunning) stopStopwatch() else startStopwatch()
                    },
                    onResetStopwatch = { resetStopwatch() },
                    onSaveGoalsAndTime = { category, minGoal, maxGoal ->
                        saveGoalsAndStopwatchTime(category, minGoal, maxGoal)
                    },
                    // Pomodoro
                    pomodoroPhase = pomodoroPhase,
                    pomodoroRemainingTime = pomodoroRemainingMs,
                    pomodoroTotalPhaseDuration = pomodoroTotalDurationMs,
                    isPomodoroRunning = isPomodoroRunning,
                    pomodoroRoundsCompleted = pomodoroRoundsCompleted,
                    onTogglePomodoro = {
                        if (isPomodoroRunning) stopPomodoro() else startPomodoro()
                    },
                    onResetPomodoro = { resetPomodoro() },
                    onSkipPomodoroPhase = { skipPomodoroPhase() },
                    onSelectPomodoroPreset = { focusM, breakM ->
                        setPomodoroPreset(focusM, breakM)
                    },
                    onSavePomodoroSession = { category ->
                        savePomodoroSession(category)
                    },
                    // Common
                    selectedCategory = selectedCategoryName,
                    onSelectCategory = { selectedCategoryName = it },
                    onBackClick = { finish() }
                )
            }
        }

        categoryViewModel.loadCategories()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- Stopwatch Controls ---
    private fun startStopwatch() {
        stopwatchStartTime = SystemClock.elapsedRealtime()
        handler.postDelayed(stopwatchRunnable, 1000)
        isStopwatchRunning = true
    }

    private fun stopStopwatch() {
        handler.removeCallbacks(stopwatchRunnable)
        isStopwatchRunning = false
        TimerForegroundService.stop(this)
    }

    private fun resetStopwatch() {
        stopStopwatch()
        stopwatchElapsedMs = 0L
    }

    private fun saveGoalsAndStopwatchTime(category: String, minGoal: Float, maxGoal: Float) {
        if (minGoal <= maxGoal) {
            val userId = authRepository.currentUserId
            userRepository.updateUserGoals(userId, minGoal, maxGoal) {
                // Goals updated
            }

            if (category.isNotEmpty() && !authRepository.isMockSession) {
                val totalTimeRef = firestore.collection("Category").document(category)
                totalTimeRef.get().addOnSuccessListener { doc ->
                    val currentTotal = doc.getLong("totalTime") ?: 0L
                    totalTimeRef.update("totalTime", currentTotal + stopwatchElapsedMs)
                }
            }
            Toast.makeText(this, "Goals and stopwatch session saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter valid goals (Min <= Max)", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Pomodoro Controls ---
    private fun startPomodoro() {
        pomodoroLastTickTime = SystemClock.elapsedRealtime()
        handler.postDelayed(pomodoroRunnable, 1000)
        isPomodoroRunning = true
    }

    private fun stopPomodoro() {
        handler.removeCallbacks(pomodoroRunnable)
        isPomodoroRunning = false
        TimerForegroundService.stop(this)
    }

    private fun resetPomodoro() {
        stopPomodoro()
        pomodoroRemainingMs = pomodoroTotalDurationMs
    }

    private fun setPomodoroPreset(focusMinutes: Int, breakMinutes: Int) {
        stopPomodoro()
        pomodoroFocusMinutes = focusMinutes
        pomodoroBreakMinutes = breakMinutes
        pomodoroPhase = PomodoroPhase.FOCUS
        pomodoroTotalDurationMs = focusMinutes * 60 * 1000L
        pomodoroRemainingMs = pomodoroTotalDurationMs
    }

    private fun skipPomodoroPhase() {
        stopPomodoro()
        if (pomodoroPhase == PomodoroPhase.FOCUS) {
            pomodoroPhase = PomodoroPhase.BREAK
            pomodoroTotalDurationMs = pomodoroBreakMinutes * 60 * 1000L
            pomodoroRemainingMs = pomodoroTotalDurationMs
        } else {
            pomodoroPhase = PomodoroPhase.FOCUS
            pomodoroTotalDurationMs = pomodoroFocusMinutes * 60 * 1000L
            pomodoroRemainingMs = pomodoroTotalDurationMs
        }
    }

    private fun onPomodoroPhaseFinished() {
        stopPomodoro()
        if (pomodoroPhase == PomodoroPhase.FOCUS) {
            pomodoroRoundsCompleted++
            pomodoroPhase = PomodoroPhase.BREAK
            pomodoroTotalDurationMs = pomodoroBreakMinutes * 60 * 1000L
            pomodoroRemainingMs = pomodoroTotalDurationMs
            Toast.makeText(this, "🎉 Focus round complete! Take a ${pomodoroBreakMinutes}m break.", Toast.LENGTH_LONG).show()
        } else {
            pomodoroPhase = PomodoroPhase.FOCUS
            pomodoroTotalDurationMs = pomodoroFocusMinutes * 60 * 1000L
            pomodoroRemainingMs = pomodoroTotalDurationMs
            Toast.makeText(this, "Break is over! Ready for round ${pomodoroRoundsCompleted + 1}?", Toast.LENGTH_LONG).show()
        }
    }

    private fun savePomodoroSession(category: String) {
        if (category.isBlank()) {
            Toast.makeText(this, "Please select a category first", Toast.LENGTH_SHORT).show()
            return
        }

        if (pomodoroAccumulatedFocusMs <= 0L) {
            Toast.makeText(this, "No focus time logged yet to save", Toast.LENGTH_SHORT).show()
            return
        }

        if (!authRepository.isMockSession) {
            val totalTimeRef = firestore.collection("Category").document(category)
            totalTimeRef.get().addOnSuccessListener { doc ->
                val currentTotal = doc.getLong("totalTime") ?: 0L
                totalTimeRef.update("totalTime", currentTotal + pomodoroAccumulatedFocusMs)
            }
        }
        val minutesLogged = pomodoroAccumulatedFocusMs / (1000 * 60)
        Toast.makeText(this, "Logged $minutesLogged minutes of focus to '$category'!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopwatchRunnable)
        handler.removeCallbacks(pomodoroRunnable)
        TimerForegroundService.stop(this)
        super.onDestroy()
    }
}
