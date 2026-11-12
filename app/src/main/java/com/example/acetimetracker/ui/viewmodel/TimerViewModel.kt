package com.example.acetimetracker.ui.viewmodel

import android.app.Application
import android.content.Context
import org.json.JSONObject
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.SettingsRepository
import com.example.acetimetracker.data.repository.TimesheetRepository
import com.example.acetimetracker.service.TimerForegroundService
import com.example.acetimetracker.ui.screen.PomodoroPhase
import com.example.acetimetracker.ui.screen.TimerMode
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Owns stopwatch and Pomodoro state so a running session survives rotation and other configuration
 * changes. Elapsed time is measured with [SystemClock.elapsedRealtime] anchors rather than by
 * counting ticks, so a delayed tick never loses time.
 */
class TimerViewModel @JvmOverloads constructor(
    application: Application,
    private val entrySaver: ((String, TimesheetEntry, (Resource<Unit>) -> Unit) -> Unit)? = null
) : AndroidViewModel(application) {

    private val timesheetRepository = TimesheetRepository()
    private val authRepository = AuthRepository()
    private val settingsRepository = SettingsRepository(application)
    private val ownerId = authRepository.currentUserId
    private val drafts = application.getSharedPreferences("timer_drafts", Context.MODE_PRIVATE)
    private val draftWriter = UUID.randomUUID().toString()
    private var stopwatchEntryId = UUID.randomUUID().toString()
    private var pomodoroEntryId = UUID.randomUUID().toString()

    var mode by mutableStateOf(TimerMode.STOPWATCH)
        private set
    var selectedCategory by mutableStateOf("")
        private set
    var isSaving by mutableStateOf(false)
        private set

    /** One-shot user-facing message; the UI shows it and then calls [consumeMessage]. */
    var message by mutableStateOf<String?>(null)
        private set

    // Stopwatch
    var isStopwatchRunning by mutableStateOf(false)
        private set
    var stopwatchElapsedMs by mutableLongStateOf(0L)
        private set
    private var stopwatchBaseMs = 0L
    private var stopwatchAnchor = 0L
    private var stopwatchSessionStart: Date? = null

    // Pomodoro
    var isPomodoroRunning by mutableStateOf(false)
        private set
    var pomodoroPhase by mutableStateOf(PomodoroPhase.FOCUS)
        private set
    var pomodoroFocusMinutes by mutableIntStateOf(25)
        private set
    var pomodoroBreakMinutes by mutableIntStateOf(5)
        private set
    var pomodoroTotalDurationMs by mutableLongStateOf(25 * 60 * 1000L)
        private set
    var pomodoroRemainingMs by mutableLongStateOf(25 * 60 * 1000L)
        private set
    var pomodoroRoundsCompleted by mutableIntStateOf(0)
        private set
    var pomodoroFocusLoggedMs by mutableLongStateOf(0L)
        private set
    private var pomodoroAnchor = 0L
    private var pomodoroSessionStart: Date? = null

    private var tickerJob: Job? = null
    private var lastNotifiedSecond = -1L

    val isAnyRunning: Boolean get() = isStopwatchRunning || isPomodoroRunning

    /** Time that a save would record for the current mode. */
    val unsavedMs: Long
        get() = if (mode == TimerMode.STOPWATCH) stopwatchElapsedMs else pomodoroFocusLoggedMs

    fun selectCategory(name: String) {
        if (isSaving) return
        selectedCategory = name
        persistDraft()
    }

    fun changeMode(newMode: TimerMode) {
        if (isSaving || mode == newMode) return
        pauseStopwatch()
        pausePomodoro()
        mode = newMode
        persistDraft()
    }

    fun consumeMessage() {
        message = null
    }

    // --- Stopwatch ---

    fun toggleStopwatch() {
        if (isSaving) return
        if (isStopwatchRunning) pauseStopwatch() else startStopwatch()
    }

    private fun startStopwatch() {
        if (stopwatchSessionStart == null) stopwatchSessionStart = Date()
        stopwatchAnchor = SystemClock.elapsedRealtime()
        isStopwatchRunning = true
        ensureTicker()
    }

    private fun pauseStopwatch() {
        if (!isStopwatchRunning) return
        stopwatchBaseMs += SystemClock.elapsedRealtime() - stopwatchAnchor
        stopwatchElapsedMs = stopwatchBaseMs
        isStopwatchRunning = false
        onTimerStopped()
    }

    fun resetStopwatch() {
        if (isSaving) return
        pauseStopwatch()
        stopwatchBaseMs = 0L
        stopwatchElapsedMs = 0L
        stopwatchSessionStart = null
        stopwatchEntryId = UUID.randomUUID().toString()
        persistDraft()
    }

    // --- Pomodoro ---

    fun togglePomodoro() {
        if (isSaving) return
        if (isPomodoroRunning) pausePomodoro() else startPomodoro()
    }

    private fun startPomodoro() {
        if (pomodoroSessionStart == null && pomodoroPhase == PomodoroPhase.FOCUS) pomodoroSessionStart = Date()
        pomodoroAnchor = SystemClock.elapsedRealtime()
        isPomodoroRunning = true
        ensureTicker()
    }

    private fun pausePomodoro() {
        if (!isPomodoroRunning) return
        advancePomodoro()
        isPomodoroRunning = false
        onTimerStopped()
    }

    fun resetPomodoro() {
        if (isSaving) return
        pausePomodoro()
        pomodoroRemainingMs = pomodoroTotalDurationMs
        persistDraft()
    }

    fun selectPomodoroPreset(focusMinutes: Int, breakMinutes: Int) {
        if (isSaving || focusMinutes !in 1..180 || breakMinutes !in 1..60) return
        pausePomodoro()
        pomodoroFocusMinutes = focusMinutes
        pomodoroBreakMinutes = breakMinutes
        setPhase(PomodoroPhase.FOCUS)
    }

    fun skipPomodoroPhase() {
        if (isSaving) return
        pausePomodoro()
        setPhase(if (pomodoroPhase == PomodoroPhase.FOCUS) PomodoroPhase.BREAK else PomodoroPhase.FOCUS)
    }

    private fun setPhase(phase: PomodoroPhase) {
        pomodoroPhase = phase
        pomodoroTotalDurationMs = (if (phase == PomodoroPhase.FOCUS) pomodoroFocusMinutes else pomodoroBreakMinutes) * 60 * 1000L
        pomodoroRemainingMs = pomodoroTotalDurationMs
        persistDraft()
    }

    /** Applies time elapsed since the last anchor to the Pomodoro countdown. */
    private fun advancePomodoro() {
        val now = SystemClock.elapsedRealtime()
        val delta = (now - pomodoroAnchor).coerceAtMost(pomodoroRemainingMs)
        pomodoroAnchor = now
        if (pomodoroPhase == PomodoroPhase.FOCUS) pomodoroFocusLoggedMs += delta
        pomodoroRemainingMs = (pomodoroRemainingMs - delta).coerceAtLeast(0L)
    }

    private fun onPomodoroPhaseFinished() {
        isPomodoroRunning = false
        onTimerStopped()
        if (pomodoroPhase == PomodoroPhase.FOCUS) {
            pomodoroRoundsCompleted++
            setPhase(PomodoroPhase.BREAK)
            message = "Focus round complete. Take a ${pomodoroBreakMinutes}-minute break."
        } else {
            setPhase(PomodoroPhase.FOCUS)
            message = "Break's over. Ready for round ${pomodoroRoundsCompleted + 1}?"
        }
    }

    // --- Ticking & notification ---

    private fun ensureTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive && isAnyRunning) {
                tick()
                delay(200)
            }
        }
    }

    private fun tick() {
        if (isStopwatchRunning) {
            stopwatchElapsedMs = stopwatchBaseMs + (SystemClock.elapsedRealtime() - stopwatchAnchor)
            notifyIfSecondChanged(stopwatchElapsedMs / 1000, TimeUtils.formatDuration(stopwatchElapsedMs), "Stopwatch")
        }
        if (isPomodoroRunning) {
            advancePomodoro()
            val totalSec = pomodoroRemainingMs / 1000
            val text = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
            val label = if (pomodoroPhase == PomodoroPhase.FOCUS) "Focus" else "Break"
            notifyIfSecondChanged(totalSec, text, label)
            if (pomodoroRemainingMs <= 0L) onPomodoroPhaseFinished()
        }
    }

    private fun notifyIfSecondChanged(second: Long, timeText: String, modeText: String) {
        if (second == lastNotifiedSecond) return
        lastNotifiedSecond = second
        persistDraft()
        if (settingsRepository.isNotificationsEnabled()) {
            TimerForegroundService.startOrUpdate(getApplication(), selectedCategory, timeText, modeText)
        }
    }

    private fun onTimerStopped() {
        lastNotifiedSecond = -1L
        if (!isAnyRunning) {
            tickerJob?.cancel()
            tickerJob = null
            TimerForegroundService.stop(getApplication())
        }
        persistDraft()
    }

    // --- Saving ---

    /**
     * Saves the current mode's tracked time as a timesheet entry, then resets that timer so the
     * same time can't be saved twice.
     */
    fun saveSession() {
        if (isSaving) return
        if (ownerId.isEmpty() || authRepository.currentUserId != ownerId) {
            message = "Sign in again before saving this session"
            return
        }
        if (selectedCategory.isBlank()) {
            message = "Choose a category first"
            return
        }

        val isStopwatch = mode == TimerMode.STOPWATCH
        if (isStopwatch) pauseStopwatch() else pausePomodoro()

        val durationMs = if (isStopwatch) stopwatchElapsedMs else pomodoroFocusLoggedMs
        if (durationMs < 1000L) {
            message = "Nothing to save yet. Start the timer first."
            return
        }

        val end = Date()
        val start = (if (isStopwatch) stopwatchSessionStart else pomodoroSessionStart)
            ?: Date(end.time - durationMs)
        val category = selectedCategory
        val entry = TimesheetEntry(
            id = if (isStopwatch) stopwatchEntryId else pomodoroEntryId,
            category = category,
            startDate = TimeUtils.formatDate(start),
            startTime = TimeUtils.formatTime(start),
            endDate = TimeUtils.formatDate(end),
            endTime = TimeUtils.formatTime(end),
            timeSpent = durationMs,
            description = if (isStopwatch) "Stopwatch session" else "Focus session ($pomodoroRoundsCompleted rounds)"
        )

        isSaving = true
        persistDraft()
        val save = entrySaver ?: timesheetRepository::saveTimesheetEntry
        viewModelScope.launch {
            // A background commit waits for preceding apply() writes before a remote save can begin.
            val durable = withContext(Dispatchers.IO) { drafts.edit().commit() }
            if (!durable) {
                isSaving = false
                message = "Could not preserve this session on the device. Please try again."
                return@launch
            }
            save(ownerId, entry) { result ->
                when (result) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        isSaving = false
                        if (isStopwatch) {
                            resetStopwatch()
                        } else {
                            pomodoroFocusLoggedMs = 0L
                            pomodoroRoundsCompleted = 0
                            pomodoroSessionStart = null
                            pomodoroEntryId = UUID.randomUUID().toString()
                            resetPomodoro()
                        }
                        message = "Saved ${TimeUtils.formatDurationShort(durationMs)} to $category"
                    }
                    is Resource.Error -> {
                        isSaving = false
                        message = result.message
                    }
                }
            }
        }
    }


    init {
        if (ownerId.isNotEmpty()) drafts.edit().putString("writer:$ownerId", draftWriter).apply()
        restoreDraft()
    }

    /** Persist a checkpoint, not a running wall clock: recovery always requires explicit resume. */
    private fun persistDraft() {
        if (ownerId.isEmpty() || drafts.getString("writer:$ownerId", null) != draftWriter) return
        val snapshot = JSONObject()
            .put("stopwatchEntryId", stopwatchEntryId)
            .put("pomodoroEntryId", pomodoroEntryId)
            .put("mode", mode.name)
            .put("category", selectedCategory)
            .put("stopwatchMs", stopwatchElapsedMs)
            .put("stopwatchStart", stopwatchSessionStart?.time ?: 0L)
            .put("phase", pomodoroPhase.name)
            .put("focusMinutes", pomodoroFocusMinutes)
            .put("breakMinutes", pomodoroBreakMinutes)
            .put("remainingMs", pomodoroRemainingMs)
            .put("rounds", pomodoroRoundsCompleted)
            .put("focusMs", pomodoroFocusLoggedMs)
            .put("focusStart", pomodoroSessionStart?.time ?: 0L)
        drafts.edit().putString(ownerId, snapshot.toString()).apply()
    }

    private fun restoreDraft() {
        if (ownerId.isEmpty()) return
        val raw = drafts.getString(ownerId, null) ?: return
        try {
            val snapshot = JSONObject(raw)
            stopwatchEntryId = snapshot.optString("stopwatchEntryId").ifBlank { UUID.randomUUID().toString() }
            pomodoroEntryId = snapshot.optString("pomodoroEntryId").ifBlank { UUID.randomUUID().toString() }
            mode = TimerMode.valueOf(snapshot.getString("mode"))
            selectedCategory = snapshot.optString("category")
            stopwatchElapsedMs = snapshot.optLong("stopwatchMs").coerceAtLeast(0)
            stopwatchBaseMs = stopwatchElapsedMs
            stopwatchSessionStart = snapshot.optLong("stopwatchStart").takeIf { it > 0 }?.let(::Date)
            pomodoroPhase = PomodoroPhase.valueOf(snapshot.getString("phase"))
            pomodoroFocusMinutes = snapshot.optInt("focusMinutes", 25).coerceIn(1, 180)
            pomodoroBreakMinutes = snapshot.optInt("breakMinutes", 5).coerceIn(1, 60)
            pomodoroTotalDurationMs = (if (pomodoroPhase == PomodoroPhase.FOCUS) pomodoroFocusMinutes else pomodoroBreakMinutes) * 60_000L
            pomodoroRemainingMs = snapshot.optLong("remainingMs", pomodoroTotalDurationMs).coerceIn(0, pomodoroTotalDurationMs)
            pomodoroRoundsCompleted = snapshot.optInt("rounds").coerceAtLeast(0)
            pomodoroFocusLoggedMs = snapshot.optLong("focusMs").coerceAtLeast(0)
            pomodoroSessionStart = snapshot.optLong("focusStart").takeIf { it > 0 }?.let(::Date)
            if (stopwatchElapsedMs > 0 || pomodoroFocusLoggedMs > 0) {
                message = "Paused draft restored. Resume or save when ready."
            }
        } catch (_: IllegalArgumentException) {
            drafts.edit().remove(ownerId).apply()
        } catch (_: org.json.JSONException) {
            drafts.edit().remove(ownerId).apply()
        }
    }

    override fun onCleared() {
        pauseStopwatch()
        pausePomodoro()
        persistDraft()
        TimerForegroundService.stop(getApplication())
        super.onCleared()
    }
}
