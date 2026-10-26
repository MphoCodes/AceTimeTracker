package com.example.acetimetracker.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.acetimetracker.R
import com.example.acetimetracker.data.repository.UserRepository
import com.example.acetimetracker.ui.viewmodel.CategoryViewModel
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils
import com.google.firebase.firestore.FirebaseFirestore

class TimerActivity : AppCompatActivity() {

    private val categoryViewModel: CategoryViewModel by viewModels()
    private val userRepository = UserRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var resetButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var editTextMinGoal: EditText
    private lateinit var editTextMaxGoal: EditText
    private lateinit var feedbackTextView: TextView
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private val categoryNames = mutableListOf<String>()

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var elapsedTime = 0L
    private var startTime = 0L

    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = SystemClock.elapsedRealtime()
            elapsedTime += currentTime - startTime
            startTime = currentTime
            timerTextView.text = TimeUtils.formatDuration(elapsedTime)

            checkGoals()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        editTextMinGoal = findViewById(R.id.editTextMinGoal)
        editTextMaxGoal = findViewById(R.id.editTextMaxGoal)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.resetButton)
        categorySpinner = findViewById(R.id.spinnerCategory)
        val saveButton = findViewById<Button>(R.id.saveButton)
        feedbackTextView = findViewById(R.id.feedbackTextView)

        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        btnBack.setOnClickListener {
            onBackPressed()
        }

        startButton.setOnClickListener {
            if (isRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }

        saveButton.setOnClickListener {
            saveGoalsAndTime()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        observeCategories()
        categoryViewModel.loadCategories()
    }

    private fun observeCategories() {
        categoryViewModel.categoriesState.observe(this) { resource ->
            if (resource is Resource.Success) {
                categoryNames.clear()
                resource.data.forEach { categoryNames.add(it.name) }
                spinnerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun startTimer() {
        startTime = SystemClock.elapsedRealtime()
        handler.postDelayed(timerRunnable, 1000)
        isRunning = true
        startButton.text = "Stop"
    }

    private fun stopTimer() {
        handler.removeCallbacks(timerRunnable)
        isRunning = false
        startButton.text = "Start"
    }

    private fun resetTimer() {
        stopTimer()
        elapsedTime = 0L
        timerTextView.text = TimeUtils.formatDuration(elapsedTime)
        feedbackTextView.text = ""
        feedbackTextView.clearAnimation()
    }

    private fun checkGoals() {
        val minGoal = editTextMinGoal.text.toString().toFloatOrNull()
        val maxGoal = editTextMaxGoal.text.toString().toFloatOrNull()
        if (minGoal == null || maxGoal == null) return

        val currentHours = elapsedTime / (1000f * 60f * 60f)
        if (currentHours >= maxGoal) {
            feedbackTextView.text = "You've reached your maximum goal! Time for a break!"
            feedbackTextView.setTextColor(ContextCompat.getColor(this, R.color.green))
            animateFeedback()
        } else if (currentHours >= minGoal) {
            feedbackTextView.text = "You're on track! Keep going!"
            feedbackTextView.setTextColor(ContextCompat.getColor(this, R.color.green))
            animateFeedback()
        } else {
            feedbackTextView.text = "You're a bit behind your minimum goal. Keep working!"
            feedbackTextView.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            animateFeedback()
        }
    }

    private fun animateFeedback() {
        val anim: Animation = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        feedbackTextView.startAnimation(anim)
    }

    private fun saveGoalsAndTime() {
        val minGoal = editTextMinGoal.text.toString().toFloatOrNull()
        val maxGoal = editTextMaxGoal.text.toString().toFloatOrNull()
        val selectedCategory = categorySpinner.selectedItem?.toString().orEmpty()

        if (minGoal != null && maxGoal != null && minGoal <= maxGoal) {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            userRepository.updateUserGoals(userId, minGoal, maxGoal) {
                // Goals updated
            }

            if (selectedCategory.isNotEmpty()) {
                val totalTimeRef = firestore.collection("Category").document(selectedCategory)
                totalTimeRef.get().addOnSuccessListener { doc ->
                    val currentTotal = doc.getLong("totalTime") ?: 0L
                    totalTimeRef.update("totalTime", currentTotal + elapsedTime)
                }
            }
            Toast.makeText(this, "Goals and time saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter valid goals (Min <= Max)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}
