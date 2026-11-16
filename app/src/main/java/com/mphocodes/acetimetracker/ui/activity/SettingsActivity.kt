package com.mphocodes.acetimetracker.ui.activity

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mphocodes.acetimetracker.data.repository.SettingsRepository
import com.mphocodes.acetimetracker.reminder.ReminderSchedule
import com.mphocodes.acetimetracker.reminder.ReminderScheduler
import com.mphocodes.acetimetracker.ui.screen.SettingsScreen
import com.mphocodes.acetimetracker.ui.theme.AceTimeTrackerTheme

class SettingsActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private var reminder by mutableStateOf(ReminderSchedule())
    private var notificationsAllowed by mutableStateOf(true)
    private var pendingReminder: ReminderSchedule? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = NotificationManagerCompat.from(this).areNotificationsEnabled()
        pendingReminder?.let { saveReminder(it.copy(enabled = granted && notificationsAllowed)) }
        pendingReminder = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        reminder = settingsRepository.getReminderSchedule()
        setContent {
            AceTimeTrackerTheme {
                SettingsScreen(
                    currentTheme = settingsRepository.getTheme(),
                    initialNotificationsEnabled = settingsRepository.isNotificationsEnabled(),
                    onBackClick = { finish() },
                    onThemeChange = settingsRepository::saveTheme,
                    onNotificationsChange = settingsRepository::saveNotificationsEnabled,
                    reminder = reminder,
                    notificationsAllowed = notificationsAllowed,
                    onReminderChange = ::requestReminder,
                    onReminderTimeClick = {
                        TimePickerDialog(this, { _, hour, minute ->
                            saveReminder(reminder.copy(hour = hour, minute = minute))
                        }, reminder.hour, reminder.minute, DateFormat.is24HourFormat(this)).show()
                    },
                    onNotificationSettingsClick = {
                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationsAllowed = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (::settingsRepository.isInitialized) ReminderScheduler.reschedule(this)
    }

    private fun requestReminder(schedule: ReminderSchedule) {
        if (schedule.enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminder = schedule
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            saveReminder(schedule)
        }
    }

    private fun saveReminder(schedule: ReminderSchedule) {
        settingsRepository.saveReminderSchedule(schedule)
        reminder = settingsRepository.getReminderSchedule()
    }
}
