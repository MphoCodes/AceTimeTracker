package com.example.acetimetracker

import android.os.SystemClock
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityBackTest {
    @Test
    fun backClosesDrawerBeforeFinishing() = verifyBack { scenario ->
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun systemBackClosesDrawerBeforeFinishing() = verifyBack {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent KEYCODE_BACK").use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
    }

    private fun verifyBack(back: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var activity: MainActivity
            scenario.onActivity {
                activity = it
                it.findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(GravityCompat.START, false)
            }
            back(scenario)
            val deadline = SystemClock.uptimeMillis() + 5000
            var drawerOpen = true
            while (drawerOpen && SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity {
                    drawerOpen = it.findViewById<DrawerLayout>(R.id.drawer_layout).isDrawerOpen(GravityCompat.START)
                    assertFalse(it.isFinishing)
                }
                SystemClock.sleep(20)
            }
            assertFalse(drawerOpen)
            back(scenario)
            while (scenario.state != Lifecycle.State.DESTROYED && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(20)
            }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            assertFalse(activity.isChangingConfigurations)
        }
    }
}
