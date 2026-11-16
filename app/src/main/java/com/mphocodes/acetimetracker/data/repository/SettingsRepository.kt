package com.mphocodes.acetimetracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mphocodes.acetimetracker.reminder.ReminderSchedule
import com.mphocodes.acetimetracker.reminder.ReminderScheduler
import java.time.DayOfWeek
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_THEME = "theme"
        private const val KEY_NOTIFICATIONS = "notifications"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    fun getTheme(): String {
        return sharedPreferences.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply()
        ThemeState.theme = theme
    }

    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun saveNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun getReminderSchedule(): ReminderSchedule = ReminderSchedule(
        enabled = sharedPreferences.getBoolean("reminder_enabled", false),
        hour = sharedPreferences.getInt("reminder_hour", 17).coerceIn(0, 23),
        minute = sharedPreferences.getInt("reminder_minute", 0).coerceIn(0, 59),
        days = sharedPreferences.getStringSet("reminder_days", setOf("1", "2", "3", "4", "5"))
            .orEmpty().mapNotNull { value ->
                value.toIntOrNull()?.takeIf { it in 1..7 }?.let(DayOfWeek::of)
            }.toSet()
    )

    fun saveReminderSchedule(schedule: ReminderSchedule) {
        sharedPreferences.edit()
            .putBoolean("reminder_enabled", schedule.enabled && schedule.days.isNotEmpty())
            .putInt("reminder_hour", schedule.hour)
            .putInt("reminder_minute", schedule.minute)
            .putStringSet("reminder_days", schedule.days.map { it.value.toString() }.toSet())
            .apply()
        ReminderScheduler.reschedule(appContext)
    }

    /** Loads the persisted theme into [ThemeState] so every activity's Compose theme picks it up. */
    fun applyTheme(theme: String = getTheme()) {
        ThemeState.theme = theme
    }
}

/** App-wide, observable theme preference read by [com.mphocodes.acetimetracker.ui.theme.AceTimeTrackerTheme]. */
object ThemeState {
    var theme by mutableStateOf(SettingsRepository.THEME_SYSTEM)
}
