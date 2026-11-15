package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/** Retains legacy entry points while sharing a single tab and timer owner. */
class TimerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java)
            .putExtra("open_tab", "Timer")
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
