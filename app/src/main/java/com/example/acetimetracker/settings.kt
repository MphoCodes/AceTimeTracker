package com.example.acetimetracker

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class settings : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var notificationsSwitch: Switch
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnBack = findViewById(R.id.btnBack)
        themeRadioGroup = findViewById(R.id.themeRadioGroup)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        val saveSettingsButton = findViewById<Button>(R.id.saveSettingsButton)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)

        // Load saved settings
        loadSettings()

        saveSettingsButton.setOnClickListener {
            saveSettings()
            applySettings()
        }
    }

    private fun loadSettings() {
        val savedTheme = sharedPreferences.getString("theme", "light") // Default to light theme
        val savedNotifications = sharedPreferences.getBoolean("notifications", true) // Default to notifications on

        when (savedTheme) {
            "light" -> themeRadioGroup.check(R.id.lightThemeRadioButton)
            "dark" -> themeRadioGroup.check(R.id.darkThemeRadioButton)
        }

        notificationsSwitch.isChecked = savedNotifications
    }

    private fun saveSettings() {
        val selectedTheme = when (themeRadioGroup.checkedRadioButtonId) {
            R.id.lightThemeRadioButton -> "light"
            R.id.darkThemeRadioButton -> "dark"
            else -> "light" // Default to light theme
        }

        val notificationsEnabled = notificationsSwitch.isChecked

        // Save settings to SharedPreferences
        with(sharedPreferences.edit()) {
            putString("theme", selectedTheme)
            putBoolean("notifications", notificationsEnabled)
            apply()
        }

        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
    }

    private fun applySettings() {
        val theme = sharedPreferences.getString("theme", "light")

        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}