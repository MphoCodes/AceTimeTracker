package com.example.acetimetracker

import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.view.inputmethod.InputMethodManager
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device coverage supplements synthetic fixtures, including pre-30 IME delivery. */
@RunWith(AndroidJUnit4::class)
class LegacyWindowRuntimeTest {
    @Test
    fun focusedPasswordRemainsAboveKeyboard() = verifyFocusedField(Login::class.java, R.id.etPassword)

    @Test
    fun focusedConfirmationRemainsAboveKeyboard() = verifyFocusedField(Registration::class.java, R.id.etConfirmPassword)

    private fun <A : AppCompatActivity> verifyFocusedField(activityClass: Class<A>, fieldId: Int) {
        ActivityScenario.launch(activityClass).use { scenario ->
            closeSoftKeyboard()
            onView(withId(fieldId)).check(matches(isDisplayed()))
            var windowBottom = 0
            scenario.onActivity {
                windowBottom = bottomOnScreen(it.window.decorView)
                val password = it.findViewById<EditText>(fieldId)
                password.requestFocus()
                (it.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(password, InputMethodManager.SHOW_IMPLICIT)
            }
            await("focused password fully visible above keyboard") {
                var ready = false
                scenario.onActivity {
                    val password = it.findViewById<EditText>(fieldId)
                    val insets = ViewCompat.getRootWindowInsets(it.window.decorView)!!
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val bottom = bottomOnScreen(password)
                    ready = insets.isVisible(WindowInsetsCompat.Type.ime()) && password.hasFocus() &&
                        bottom <= windowBottom - ime
                    Log.i("P1Runtime", "Focused password: bottom=$bottom, keyboardTop=${windowBottom - ime}, focus=${password.hasFocus()}")
                }
                ready
            }
        }
    }

    @Test
    fun realKeyboardShowHideRestoresContentPadding() {
        ActivityScenario.launch(Login::class.java).use { scenario ->
            var initialBottom = 0
            var windowBottom = 0
            var initialUsableBottom = 0
            closeSoftKeyboard()
            await("initial layout and bar insets settled") {
                var ready = false
                scenario.onActivity {
                    val decor = it.window.decorView
                    val content = it.findViewById<ViewGroup>(android.R.id.content)
                    val insets = ViewCompat.getRootWindowInsets(decor)
                    ready = decor.hasWindowFocus() && content.isLaidOut && insets != null &&
                        !insets.isVisible(WindowInsetsCompat.Type.ime()) &&
                        content.paddingBottom == insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                    if (ready) {
                        initialBottom = content.paddingBottom
                        windowBottom = bottomOnScreen(decor)
                        initialUsableBottom = bottomOnScreen(content.getChildAt(0))
                    }
                }
                ready
            }
            onView(withId(R.id.etEmail)).perform(click())
            await("keyboard visible and content above it") {
                var ready = false
                scenario.onActivity {
                    val content = it.findViewById<ViewGroup>(android.R.id.content)
                    val insets = ViewCompat.getRootWindowInsets(it.window.decorView)!!
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val usableBottom = bottomOnScreen(content.getChildAt(0))
                    ready = insets.isVisible(WindowInsetsCompat.Type.ime()) && ime > initialBottom &&
                        content.paddingBottom == ime && usableBottom == windowBottom - ime
                    Log.i("P1Runtime", "IME check: bottom=$ime, contentHeight=${content.height}, usableBottom=$usableBottom, expected=${windowBottom - ime}")
                }
                ready
            }
            systemBack()
            await("keyboard hidden and original bar padding restored") {
                var ready = false
                scenario.onActivity {
                    val content = it.findViewById<ViewGroup>(android.R.id.content)
                    val insets = ViewCompat.getRootWindowInsets(it.window.decorView)!!
                    ready = !insets.isVisible(WindowInsetsCompat.Type.ime()) && content.paddingBottom == initialBottom &&
                        bottomOnScreen(content.getChildAt(0)) == initialUsableBottom
                    if (ready) Log.i("P1Runtime", "IME hidden: bottom=${content.paddingBottom}, contentHeight=${content.height}")
                }
                ready
            }
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun registrationLoginLinkAndSystemBackPreserveFormNavigation() {
        ActivityScenario.launch(Registration::class.java).use { scenario ->
            onView(withId(R.id.etUsername)).check(matches(isDisplayed()))
            onView(withId(R.id.tvLoginLink)).perform(click())
            closeSoftKeyboard()
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
            systemBack()
            await("Registration resumed after system Back transition") {
                var ready = false
                if (scenario.state == Lifecycle.State.RESUMED) {
                    scenario.onActivity { ready = it.window.decorView.hasWindowFocus() }
                }
                ready
            }
            onView(withId(R.id.etUsername)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun visibleBackReturnsFromAboutToDrawerHost() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(GravityCompat.START, false)
            }
            onView(withId(R.id.nav_about)).perform(click())
            onView(withId(R.id.btnBack)).check(matches(isDisplayed())).perform(click())
            await("drawer host resumed") { scenario.state == Lifecycle.State.RESUMED }
            scenario.onActivity { assertTrue(it.findViewById<DrawerLayout>(R.id.drawer_layout).isShown) }
        }
    }

    private fun systemBack() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent KEYCODE_BACK").use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
    }

    private fun bottomOnScreen(view: android.view.View): Int {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location[1] + view.height
    }

    private fun await(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue("Timed out waiting for $description", condition())
    }
}
