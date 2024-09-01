package com.example.acetimetracker


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class Splash : AppCompatActivity() {
    private val SPLASH_DELAY: Long = 4000 // 4 seconds

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            // Start your main activity here
            startActivity(Intent(this@Splash, Registration::class.java))
            finish()
        }

        // Delay the execution of the Runnable
        handler.postDelayed(runnable, SPLASH_DELAY)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the Runnable callback to avoid any potential memory leaks
        handler.removeCallbacks(runnable)
    }
}