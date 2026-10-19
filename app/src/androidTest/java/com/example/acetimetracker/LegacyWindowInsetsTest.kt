package com.example.acetimetracker

import android.graphics.Rect
import android.os.Build
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.acetimetracker.core.ui.installLegacyWindowInsets
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyWindowInsetsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    // Removing the listener, adding to current padding, summing IME and bars,
    // or consuming unrelated inset types must break these real View assertions.
    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun appliesSystemBarsAndCutoutOnce() = withContent { view ->
        ViewCompat.dispatchApplyWindowInsets(view, fixture())
        assertPadding(view, 33, 29, 7, 27)
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun imeUsesMaximumBottomInsetAndResets() = withContent { view ->
        ViewCompat.dispatchApplyWindowInsets(view, fixture(imeBottom = 250))
        assertEquals(261, view.paddingBottom)
        ViewCompat.dispatchApplyWindowInsets(view, fixture())
        assertEquals(27, view.paddingBottom)
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun redispatchAndReinstallDoNotAccumulate() = withContent { view ->
        ViewCompat.dispatchApplyWindowInsets(view, fixture())
        ViewCompat.dispatchApplyWindowInsets(view, fixture())
        installLegacyWindowInsets(view)
        ViewCompat.dispatchApplyWindowInsets(view, fixture())
        assertPadding(view, 33, 29, 7, 27)
    }

    @Test
    fun hiddenBarsRestoreInitialPadding() = withContent { view ->
        ViewCompat.dispatchApplyWindowInsets(view, if (Build.VERSION.SDK_INT >= 30) fixture(250) else bars())
        ViewCompat.dispatchApplyWindowInsets(view, WindowInsetsCompat.Builder().build())
        assertPadding(view, 3, 5, 7, 11)
    }

    @Test
    fun systemBarsRedispatchAndReinstallDoNotAccumulate() = withContent { view ->
        ViewCompat.dispatchApplyWindowInsets(view, bars())
        assertPadding(view, 3, 29, 7, 27)
        installLegacyWindowInsets(view)
        ViewCompat.dispatchApplyWindowInsets(view, bars())
        ViewCompat.dispatchApplyWindowInsets(view, bars())
        assertPadding(view, 3, 29, 7, 27)
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun forwardsUnhandledInsetsToChildren() = withContent { view ->
        val child = FrameLayout(view.context)
        view.addView(child)
        var delivered: WindowInsetsCompat? = null
        ViewCompat.setOnApplyWindowInsetsListener(child) { _, insets ->
            delivered = insets
            insets
        }
        val input = WindowInsetsCompat.Builder(fixture(imeBottom = 250))
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(12, 0, 13, 16))
            .build()
        ViewCompat.dispatchApplyWindowInsets(view, input)
        assertEquals(Insets.NONE, delivered!!.getInsets(WindowInsetsCompat.Type.systemBars()))
        assertEquals(Insets.NONE, delivered!!.getInsets(WindowInsetsCompat.Type.displayCutout()))
        assertEquals(Insets.NONE, delivered!!.getInsets(WindowInsetsCompat.Type.ime()))
        assertEquals(Insets.of(12, 0, 13, 16), delivered!!.getInsets(WindowInsetsCompat.Type.systemGestures()))
    }

    private fun withContent(block: (FrameLayout) -> Unit) {
        instrumentation.runOnMainSync {
            val view = FrameLayout(instrumentation.targetContext)
            view.setPadding(3, 5, 7, 11)
            installLegacyWindowInsets(view)
            block(view)
        }
    }

    private fun bars() = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 16))
        .build()

    private fun fixture(imeBottom: Int = 0) = WindowInsetsCompat.Builder(bars())
        .setDisplayCutout(DisplayCutoutCompat(Rect(30, 0, 0, 0), emptyList()))
        .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, imeBottom))
        .build()

    private fun assertPadding(view: FrameLayout, left: Int, top: Int, right: Int, bottom: Int) {
        assertEquals(left, view.paddingLeft)
        assertEquals(top, view.paddingTop)
        assertEquals(right, view.paddingRight)
        assertEquals(bottom, view.paddingBottom)
    }
}
