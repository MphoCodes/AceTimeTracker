package com.example.acetimetracker.ui.activity

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.acetimetracker.R
import com.example.acetimetracker.data.repository.SettingsRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsRepository = SettingsRepository(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val themeRadioGroup = findViewById<RadioGroup>(R.id.themeRadioGroup)
        val notificationsSwitch = findViewById<Switch>(R.id.notificationsSwitch)
        val saveSettingsButton = findViewById<Button>(R.id.saveSettingsButton)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Load current settings
        when (settingsRepository.getTheme()) {
            SettingsRepository.THEME_LIGHT -> themeRadioGroup.check(R.id.lightThemeRadioButton)
            SettingsRepository.THEME_DARK -> themeRadioGroup.check(R.id.darkThemeRadioButton)
        }
        notificationsSwitch.isChecked = settingsRepository.isNotificationsEnabled()

        saveSettingsButton.setOnClickListener {
            val selectedTheme = when (themeRadioGroup.checkedRadioButtonId) {
                R.id.darkThemeRadioButton -> SettingsRepository.THEME_DARK
                else -> SettingsRepository.THEME_LIGHT
            }
            settingsRepository.saveTheme(selectedTheme)
            settingsRepository.saveNotificationsEnabled(notificationsSwitch.isChecked)
            settingsRepository.applyTheme(selectedTheme)

            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
        }
    }
}
