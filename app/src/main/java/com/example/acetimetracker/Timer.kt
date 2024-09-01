package com.example.acetimetracker

import android.content.ContentValues.TAG
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Timer : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var resetButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var editTextMinGoal: EditText
    private lateinit var editTextMaxGoal: EditText
    private lateinit var feedbackTextView: TextView
    private lateinit var btnBack: ImageButton
    private val handler = Handler()
    private val firestore = FirebaseFirestore.getInstance()

    private var isRunning = false
    private var elapsedTime = 0L
    private var startTime = 0L
    private var minGoal: Float? = null
    private var maxGoal: Float? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = SystemClock.elapsedRealtime()
            elapsedTime += currentTime - startTime
            startTime = currentTime
            updateTimerDisplay(elapsedTime)
            handler.postDelayed(this, 1000)

            val currentHours = elapsedTime / (1000 * 60 * 60).toFloat()
            if (minGoal != null && maxGoal != null) {
                if (currentHours >= minGoal!!) {
                    feedbackTextView.text = "You're on track! Keep going!"
                    feedbackTextView.setTextColor(ContextCompat.getColor(this@Timer, R.color.green))
                    animateFeedback()
                } else if (currentHours < minGoal!!) {
                    feedbackTextView.text = "You're a bit behind your minimum goal. Keep working!"
                    feedbackTextView.setTextColor(
                        ContextCompat.getColor(
                            this@Timer,
                            R.color.yellow
                        )
                    )
                    animateFeedback()
                }
                if (currentHours >= maxGoal!!) {
                    feedbackTextView.text = "You've reached your maximum goal! Time for a break!"
                    feedbackTextView.setTextColor(ContextCompat.getColor(this@Timer, R.color.green))
                    animateFeedback()
                }
            }
        }
    }

    // Function to animate feedback
    private fun animateFeedback() {
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 500 // Set animation duration to 500 milliseconds
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        feedbackTextView.startAnimation(anim) // Start the animation
    }

    private fun fetchCategories() {
        val categories = mutableListOf<String>()
        firestore.collection("Category")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val category = document.getString("name")
                    category?.let {
                        categories.add(category)
                    }
                }
                updateSpinnerAdapter(categories) // Call updateSpinnerAdapter
            }
            .addOnFailureListener { exception ->
                // Handle errors
                Log.e(TAG, "Error fetching categories: $exception")
                // You might want to provide feedback to the user here
            }
    }

    private fun updateSpinnerAdapter(categories: List<String>) { // Define updateSpinnerAdapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        editTextMinGoal = findViewById(R.id.editTextMinGoal)
        editTextMaxGoal = findViewById(R.id.editTextMaxGoal)
        btnBack = findViewById(R.id.btnBack)
        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.resetButton)
        categorySpinner = findViewById(R.id.spinnerCategory)
        val saveButton = findViewById<Button>(R.id.saveButton)
        feedbackTextView = findViewById(R.id.feedbackTextView)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        fetchCategories()

        startButton.setOnClickListener {
            if (isRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }
        saveButton.setOnClickListener {
            updateTotalTime()
        }

        resetButton.setOnClickListener {
            resetTimer()
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
        updateTotalTime()
    }

    private fun resetTimer() {
        stopTimer()
        elapsedTime = 0L
        updateTimerDisplay(elapsedTime)
        feedbackTextView.text = ""
        feedbackTextView.clearAnimation() // Stop animation on reset
    }

    private fun updateTimerDisplay(elapsedTime: Long) {
        val hours = elapsedTime / 3600000
        val minutes = (elapsedTime % 3600000) / 60000
        val seconds = (elapsedTime % 60000) / 1000
        val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        timerTextView.text = timeFormatted
    }

    private fun updateTotalTime() {
        val selectedCategory = categorySpinner.selectedItem.toString()
        val totalTimeRef = firestore.collection("Category").document(selectedCategory)

        val minGoal = editTextMinGoal.text.toString().toFloatOrNull()
        val maxGoal = editTextMaxGoal.text.toString().toFloatOrNull()

        if (minGoal != null && maxGoal != null && minGoal <= maxGoal) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            // Use safe cast operator and null check for minGoal and maxGoal
            val minGoalFloat = minGoal as? Float
            val maxGoalFloat = maxGoal as? Float

            if (minGoalFloat != null && maxGoalFloat != null) {
                firestore.collection("users").document(currentUserId)
                    .update(
                        "minGoal", minGoalFloat,
                        "maxGoal", maxGoalFloat
                    )
                    .addOnSuccessListener {
                        Log.d("TimerActivity", "Goals updated successfully")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("TimerActivity", "Error updating goals: ${exception.message}", exception)
                    }

                totalTimeRef.get()
                    .addOnSuccessListener { document ->
                        val currentTotalTime = document.getLong("totalTime") ?: 0L
                        val updatedTotalTime = currentTotalTime + elapsedTime

                        totalTimeRef.update("totalTime", updatedTotalTime)
                            .addOnSuccessListener {
                                Log.d(
                                    TAG,
                                    "Total time updated successfully for category: $selectedCategory"
                                )
                            }
                            .addOnFailureListener { exception ->
                                Log.e(
                                    TAG,
                                    "Error updating total time for category: $selectedCategory",
                                    exception
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error fetching total time for category: $selectedCategory", exception)
                    }
            }
        } else {
            Toast.makeText(this, "Please enter valid goals", Toast.LENGTH_SHORT).show()
        }
    }
}