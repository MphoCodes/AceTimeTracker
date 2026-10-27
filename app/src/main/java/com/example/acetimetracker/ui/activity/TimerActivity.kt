package com.example.acetimetracker.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.content.ContextCompat
import com.example.acetimetracker.ui.screen.TimerScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.CategoryViewModel
import com.example.acetimetracker.ui.viewmodel.TimerViewModel

class TimerActivity : ComponentActivity() {

    private val categoryViewModel: CategoryViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The timer works either way; the permission only controls the ongoing notification. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) checkNotificationPermission()

        setContent {
            AceTimeTrackerTheme {
                val categoriesState by categoryViewModel.categoriesState.observeAsState()
                val vm = timerViewModel

                TimerScreen(
                    categoriesState = categoriesState,
                    mode = vm.mode,
                    onModeChange = vm::changeMode,
                    stopwatchElapsedTime = vm.stopwatchElapsedMs,
                    isStopwatchRunning = vm.isStopwatchRunning,
                    onToggleStopwatch = vm::toggleStopwatch,
                    onResetStopwatch = vm::resetStopwatch,
                    pomodoroPhase = vm.pomodoroPhase,
                    pomodoroRemainingTime = vm.pomodoroRemainingMs,
                    pomodoroTotalPhaseDuration = vm.pomodoroTotalDurationMs,
                    pomodoroFocusMinutes = vm.pomodoroFocusMinutes,
                    isPomodoroRunning = vm.isPomodoroRunning,
                    pomodoroRoundsCompleted = vm.pomodoroRoundsCompleted,
                    pomodoroFocusLogged = vm.pomodoroFocusLoggedMs,
                    onTogglePomodoro = vm::togglePomodoro,
                    onResetPomodoro = vm::resetPomodoro,
                    onSkipPomodoroPhase = vm::skipPomodoroPhase,
                    onSelectPomodoroPreset = vm::selectPomodoroPreset,
                    selectedCategory = vm.selectedCategory,
                    onSelectCategory = vm::selectCategory,
                    isSaving = vm.isSaving,
                    onSaveSession = vm::saveSession,
                    message = vm.message,
                    onMessageShown = vm::consumeMessage,
                    onCreateCategoryClick = {
                        startActivity(Intent(this, CategoriesActivity::class.java))
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh so a category created from the empty-state shortcut appears on return.
        categoryViewModel.loadCategories()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
