package com.example.acetimetracker.service

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import com.example.acetimetracker.ui.activity.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class TimerNotificationTest {
    @Test fun runningActionRequestsPauseThroughAuthenticatedHost() {
        val controller = Robolectric.buildService(TimerForegroundService::class.java).create()
        val service = controller.get()
        service.onStartCommand(Intent(service, TimerForegroundService::class.java)
            .setAction(TimerForegroundService.ACTION_START_OR_UPDATE), 0, 1)
        val manager = service.getSystemService(NotificationManager::class.java)
        val notification = shadowOf(manager).getNotification(TimerForegroundService.NOTIFICATION_ID)
        val actionIntent = shadowOf(notification.actions.single().actionIntent).savedIntent
        assertEquals(MainActivity::class.java.name, actionIntent.component?.className)
        assertEquals("Timer", actionIntent.getStringExtra("open_tab"))
        assertEquals("pause", actionIntent.getStringExtra(TimerForegroundService.EXTRA_TIMER_ACTION))
        assertNull(shadowOf(notification.contentIntent).savedIntent
            .getStringExtra(TimerForegroundService.EXTRA_TIMER_ACTION))
        controller.destroy()
    }

    @Test fun pausedActionRequestsResumeAndStopClearsIt() {
        val app = RuntimeEnvironment.getApplication()
        TimerForegroundService.showPaused(app, "Development", "00:01:00", "Stopwatch")
        val manager = shadowOf(app.getSystemService(NotificationManager::class.java))
        val notification = manager.allNotifications.single()
        val actionIntent = shadowOf(notification.actions.single().actionIntent).savedIntent
        assertEquals("resume", actionIntent.getStringExtra(TimerForegroundService.EXTRA_TIMER_ACTION))
        assertEquals(MainActivity::class.java.name, actionIntent.component?.className)
        TimerForegroundService.stop(app)
        assertTrue(manager.allNotifications.isEmpty())
    }
}
