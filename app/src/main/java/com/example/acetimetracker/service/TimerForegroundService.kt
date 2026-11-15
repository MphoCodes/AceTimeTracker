package com.example.acetimetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.activity.MainActivity

class TimerForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "acetimetracker_timer_channel"
        const val NOTIFICATION_ID = 1001
        private const val PAUSED_NOTIFICATION_ID = 1002
        const val ACTION_START_OR_UPDATE = "com.example.acetimetracker.service.START_OR_UPDATE"
        const val ACTION_STOP = "com.example.acetimetracker.service.STOP"
        const val EXTRA_CATEGORY_NAME = "extra_category_name"
        const val EXTRA_TIME_TEXT = "extra_time_text"
        const val EXTRA_SESSION_MODE = "extra_session_mode"
        const val EXTRA_TIMER_ACTION = "timer_action"

        fun startOrUpdate(
            context: Context, categoryName: String, timeText: String, modeText: String,
            isRunning: Boolean = true
        ) {
            if (!isRunning) {
                showPaused(context, categoryName, timeText, modeText)
                return
            }
            NotificationManagerCompat.from(context).cancel(PAUSED_NOTIFICATION_ID)
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_TIME_TEXT, timeText)
                putExtra(EXTRA_SESSION_MODE, modeText)
            }
            try {
                context.startForegroundService(intent)
            } catch (_: Exception) {
                // Tracking remains usable if Android restricts background service starts.
            }
        }

        fun showPaused(context: Context, categoryName: String, timeText: String, modeText: String) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
            createNotificationChannel(context)
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            try {
                NotificationManagerCompat.from(context).notify(
                    PAUSED_NOTIFICATION_ID,
                    buildNotification(context, categoryName, timeText, modeText, isRunning = false)
                )
            } catch (_: SecurityException) {
                // Permission may have changed since the availability check.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            NotificationManagerCompat.from(context).cancel(PAUSED_NOTIFICATION_ID)
        }

        private fun activityIntent(context: Context, timerAction: String? = null): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", "Timer")
                if (timerAction != null) putExtra(EXTRA_TIMER_ACTION, timerAction)
            }
            // Distinct identities prevent updating the content tap into a mutation action.
            val requestCode = when (timerAction) { "pause" -> 1003; "resume" -> 1004; else -> 1000 }
            return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun buildNotification(
            context: Context, categoryName: String, timeText: String, mode: String,
            isRunning: Boolean
        ): Notification {
            val content = if (categoryName.isNotBlank() && categoryName != "Focus Session") {
                "$timeText  •  $categoryName"
            } else timeText
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("AceTimeTracker • $mode" + if (isRunning) "" else " paused")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_ace_notification)
                .setContentIntent(activityIntent(context))
                .addAction(
                    0, if (isRunning) "Pause" else "Resume",
                    activityIntent(context, if (isRunning) "pause" else "resume")
                )
                .setOngoing(isRunning)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        private fun createNotificationChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Active timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Timer status with pause and resume controls"
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_OR_UPDATE -> {
                val notification = buildNotification(
                    this,
                    intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Focus Session",
                    intent.getStringExtra(EXTRA_TIME_TEXT) ?: "00:00:00",
                    intent.getStringExtra(EXTRA_SESSION_MODE) ?: "Timer",
                    isRunning = true
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID, notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_NOT_STICKY
    }
}
