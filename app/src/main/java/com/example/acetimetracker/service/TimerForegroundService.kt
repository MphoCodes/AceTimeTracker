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
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.activity.TimerActivity

class TimerForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "acetimetracker_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_OR_UPDATE = "com.example.acetimetracker.service.START_OR_UPDATE"
        const val ACTION_STOP = "com.example.acetimetracker.service.STOP"

        const val EXTRA_CATEGORY_NAME = "extra_category_name"
        const val EXTRA_TIME_TEXT = "extra_time_text"
        const val EXTRA_SESSION_MODE = "extra_session_mode"

        fun startOrUpdate(context: Context, categoryName: String, timeText: String, modeText: String) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_TIME_TEXT, timeText)
                putExtra(EXTRA_SESSION_MODE, modeText)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Ignore service start failures if background restrictions apply
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_OR_UPDATE -> {
                val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Focus Session"
                val timeText = intent.getStringExtra(EXTRA_TIME_TEXT) ?: "00:00:00"
                val mode = intent.getStringExtra(EXTRA_SESSION_MODE) ?: "Timer"

                val notification = buildNotification(categoryName, timeText, mode)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(categoryName: String, timeText: String, mode: String): Notification {
        val openAppIntent = Intent(this, TimerActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = "⏱️ AceTimeTracker • $mode"
        val content = if (categoryName.isNotBlank() && categoryName != "Focus Session") {
            "$timeText  •  $categoryName"
        } else {
            timeText
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.acelogo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Timer Notification",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live time tracking in the notification shade"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
