package com.example.acetimetracker.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.acetimetracker.R
import com.example.acetimetracker.data.repository.AuthRepository

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashDelay: Long = 2000
    private val handler = Handler(Looper.getMainLooper())
    private val authRepository = AuthRepository()

    private val runnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            val destination = if (authRepository.isAuthenticated) {
                MainActivity::class.java
            } else {
                RegistrationActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed(runnable, splashDelay)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }
}
