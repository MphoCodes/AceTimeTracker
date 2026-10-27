package com.example.acetimetracker.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.UserRepository
import com.example.acetimetracker.ui.screen.TimerScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.CategoryViewModel
import com.google.firebase.firestore.FirebaseFirestore

class TimerActivity : ComponentActivity() {

    private val categoryViewModel: CategoryViewModel by viewModels()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning by mutableStateOf(false)
    private var elapsedTime by mutableStateOf(0L)
    private var startTime = 0L

    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = SystemClock.elapsedRealtime()
            elapsedTime += currentTime - startTime
            startTime = currentTime
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AceTimeTrackerTheme {
                val categoriesState by categoryViewModel.categoriesState.observeAsState()

                TimerScreen(
                    categoriesState = categoriesState,
                    elapsedTime = elapsedTime,
                    isRunning = isRunning,
                    onBackClick = { finish() },
                    onToggleStartStop = {
                        if (isRunning) stopTimer() else startTimer()
                    },
                    onResetTimer = { resetTimer() },
                    onSaveGoalsAndTime = { category, minGoal, maxGoal ->
                        saveGoalsAndTime(category, minGoal, maxGoal)
                    }
                )
            }
        }

        categoryViewModel.loadCategories()
    }

    private fun startTimer() {
        startTime = SystemClock.elapsedRealtime()
        handler.postDelayed(timerRunnable, 1000)
        isRunning = true
    }

    private fun stopTimer() {
        handler.removeCallbacks(timerRunnable)
        isRunning = false
    }

    private fun resetTimer() {
        stopTimer()
        elapsedTime = 0L
    }

    private fun saveGoalsAndTime(selectedCategory: String, minGoal: Float, maxGoal: Float) {
        if (minGoal <= maxGoal) {
            val userId = authRepository.currentUserId
            userRepository.updateUserGoals(userId, minGoal, maxGoal) {
                // Goals updated
            }

            if (selectedCategory.isNotEmpty() && !authRepository.isMockSession) {
                val totalTimeRef = firestore.collection("Category").document(selectedCategory)
                totalTimeRef.get().addOnSuccessListener { doc ->
                    val currentTotal = doc.getLong("totalTime") ?: 0L
                    totalTimeRef.update("totalTime", currentTotal + elapsedTime)
                }
            }
            Toast.makeText(this, "Goals and session time saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter valid goals (Min <= Max)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}
