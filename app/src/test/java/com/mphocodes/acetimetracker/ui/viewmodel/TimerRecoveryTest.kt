package com.mphocodes.acetimetracker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mphocodes.acetimetracker.AceTimeTrackerApp
import com.mphocodes.acetimetracker.data.repository.AuthRepository
import com.mphocodes.acetimetracker.ui.screen.TimerMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
class TimerRecoveryTest {
    private lateinit var app: Application

    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        app.getSharedPreferences("timer_drafts", Context.MODE_PRIVATE).edit().clear().commit()
        if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(app, com.google.firebase.FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:1234567890abcdef")
                .setApiKey("local-test-api-key")
                .setProjectId("demo-acetimetracker")
                .setStorageBucket("demo-acetimetracker.appspot.com")
                .build())
        }
        AuthRepository().loginTestAccount {}
    }

    private fun timer(
        store: ViewModelStore,
        saver: ((String, com.mphocodes.acetimetracker.data.model.TimesheetEntry, (com.mphocodes.acetimetracker.util.Resource<Unit>) -> Unit) -> Unit)? = null
    ): TimerViewModel =
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TimerViewModel(app, saver) as T
            }
        })[TimerViewModel::class.java]

    private fun awaitSave(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 3_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Save should begin after its local checkpoint is durable", condition())
    }

    @Test fun desiredRunningStateIsIdempotent() {
        val store = ViewModelStore()
        val vm = timer(store)
        vm.selectCategory("Development")
        vm.setRunning(true)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        vm.setRunning(true)
        assertTrue(vm.isStopwatchRunning)
        vm.setRunning(false)
        assertEquals(5000L, vm.stopwatchElapsedMs)
        vm.setRunning(false)
        assertEquals(5000L, vm.stopwatchElapsedMs)
        store.clear()
    }

    @Test fun repeatCannotOverwritePausedDraft() {
        val store = ViewModelStore()
        val vm = timer(store)
        vm.selectCategory("Development")
        vm.setRunning(true)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        vm.setRunning(false)
        vm.trackAgain("Design")
        assertEquals("Development", vm.selectedCategory)
        assertEquals(5000L, vm.stopwatchElapsedMs)
        assertFalse(vm.isAnyRunning)
        store.clear()
    }

    @Test fun interruptedSaveRetriesTheSameEntryIdentity() {
        val entries = mutableListOf<com.mphocodes.acetimetracker.data.model.TimesheetEntry>()
        val saver: (String, com.mphocodes.acetimetracker.data.model.TimesheetEntry, (com.mphocodes.acetimetracker.util.Resource<Unit>) -> Unit) -> Unit =
            { _, entry, _ -> entries.add(entry) }
        val firstStore = ViewModelStore()
        val first = timer(firstStore, saver)
        first.selectCategory("Development")
        first.toggleStopwatch()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        first.saveSession()
        awaitSave { entries.size == 1 }
        first.resetStopwatch()
        first.selectCategory("Must not change during save")
        assertEquals(5_000L, first.stopwatchElapsedMs)
        assertEquals("Development", first.selectedCategory)
        firstStore.clear()

        val secondStore = ViewModelStore()
        val second = timer(secondStore, saver)
        second.saveSession()
        awaitSave { entries.size == 2 }
        assertTrue(entries[0].id.isNotBlank())
        assertEquals(entries[0].id, entries[1].id)
        assertEquals(5_000L, entries[1].timeSpent)
        secondStore.clear()
    }

    @Test fun oldSaveCallbackCannotOverwriteANewerDraft() {
        var callback: ((com.mphocodes.acetimetracker.util.Resource<Unit>) -> Unit)? = null
        val firstStore = ViewModelStore()
        val first = timer(firstStore) { _, _, result -> callback = result }
        first.selectCategory("Development")
        first.toggleStopwatch()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        first.saveSession()
        awaitSave { callback != null }
        firstStore.clear()

        val secondStore = ViewModelStore()
        val second = timer(secondStore)
        second.selectCategory("New draft category")
        callback!!(com.mphocodes.acetimetracker.util.Resource.Success(Unit))
        // Recreate immediately: do not let second.onCleared hide a stale overwrite.
        val thirdStore = ViewModelStore()
        val third = timer(thirdStore)
        assertEquals("New draft category", third.selectedCategory)
        assertEquals(5_000L, third.stopwatchElapsedMs)
        secondStore.clear()
        thirdStore.clear()
    }

    @Test fun leavingTimerPreservesPausedStopwatchDraft() {
        val store = ViewModelStore()
        val first = timer(store)
        first.selectCategory("Development")
        first.toggleStopwatch()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(65))
        first.toggleStopwatch()
        store.clear()

        val restoredStore = ViewModelStore()
        val restored = timer(restoredStore)
        assertEquals(65_000L, restored.stopwatchElapsedMs)
        assertEquals("Development", restored.selectedCategory)
        assertFalse(restored.isStopwatchRunning)
        restoredStore.clear()
    }

    @Test fun leavingTimerPreservesFocusTimeWithoutCountingBreaks() {
        val store = ViewModelStore()
        val first = timer(store)
        first.changeMode(TimerMode.POMODORO)
        first.togglePomodoro()
        ShadowSystemClock.advanceBy(Duration.ofMinutes(2))
        first.togglePomodoro()
        first.skipPomodoroPhase()
        first.togglePomodoro()
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1))
        first.togglePomodoro()
        store.clear()

        val restoredStore = ViewModelStore()
        val restored = timer(restoredStore)
        assertEquals(TimerMode.POMODORO, restored.mode)
        assertEquals(120_000L, restored.pomodoroFocusLoggedMs)
        assertEquals(240_000L, restored.pomodoroRemainingMs)
        assertFalse(restored.isPomodoroRunning)
        restoredStore.clear()
    }

    @Test fun resetDoesNotBringBackOldStopwatchTime() {
        val store = ViewModelStore()
        val first = timer(store)
        first.toggleStopwatch()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        first.toggleStopwatch()
        first.resetStopwatch()
        store.clear()
        val restoredStore = ViewModelStore()
        assertEquals(0L, timer(restoredStore).stopwatchElapsedMs)
        restoredStore.clear()
    }

    @Test fun aDifferentAccountCannotSeeTheDraft() {
        val store = ViewModelStore()
        val first = timer(store)
        first.selectCategory("Private category")
        first.toggleStopwatch()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        first.toggleStopwatch()
        store.clear()
        AuthRepository().logout()
        val restoredStore = ViewModelStore()
        val restored = timer(restoredStore)
        assertEquals("", restored.selectedCategory)
        assertEquals(0L, restored.stopwatchElapsedMs)
        restoredStore.clear()
    }
}
