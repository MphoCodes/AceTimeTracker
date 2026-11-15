package com.example.acetimetracker.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.acetimetracker.R
import com.example.acetimetracker.data.repository.SettingsRepository
import com.example.acetimetracker.ui.activity.MainActivity
import java.time.ZonedDateTime

object ReminderScheduler {
    private const val REQUEST_CODE = 2101
    private const val CHANNEL_ID = "daily_tracking_reminders"
    private const val NOTIFICATION_ID = 2102
    internal const val ACTION_REMIND = "com.example.acetimetracker.REMIND"

    fun reschedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        val schedule = SettingsRepository(context).getReminderSchedule()
        if (!schedule.enabled) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }
        val next = schedule.nextOccurrence(ZonedDateTime.now()) ?: return
        // User reminders are approximate: no exact-alarm access or battery exemption.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending)
    }

    internal fun notify(context: Context) {
        if (!SettingsRepository(context).getReminderSchedule().enabled) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tracking reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context, REQUEST_CODE,
            Intent(context, MainActivity::class.java)
                .putExtra("open_tab", "Timer")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.acelogo)
            .setContentTitle("Make time for your day")
            .setContentText("Track a session or review the time you have logged.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission may be revoked between the availability check and posting.
        }
    }
}

/** Non-exported; only the app's immutable alarm PendingIntent and system broadcasts reach it. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMIND -> {
                ReminderScheduler.notify(context)
                ReminderScheduler.reschedule(context)
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                ReminderScheduler.reschedule(context)
        }
    }
}
