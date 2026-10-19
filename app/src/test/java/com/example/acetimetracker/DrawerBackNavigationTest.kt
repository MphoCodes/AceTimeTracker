package com.example.acetimetracker

import android.os.Looper
import android.os.Parcelable
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/** A real drawer and dispatcher; no Firebase clients or account state are used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class DrawerBackNavigationTest {
    private lateinit var controller: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var drawer: DrawerLayout

    @Before
    fun setUp() {
        createHost()
    }

    private fun createHost(savedState: SparseArray<Parcelable>? = null) {
        controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity = controller.get()
        drawer = DrawerLayout(activity).apply {
            id = R.id.drawer_layout
            addView(FrameLayout(activity), DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(FrameLayout(activity), DrawerLayout.LayoutParams(240, MATCH_PARENT, Gravity.START))
        }
        activity.setContentView(drawer)
        activity.handleDrawerBack(drawer)
        savedState?.let(drawer::restoreHierarchyState)
        drawer.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
        )
        drawer.layout(0, 0, 1000, 1000)
    }

    @After
    fun tearDown() {
        controller.pause().stop().destroy()
    }

    @Test
    fun backClosesOpenDrawerWithoutFinishingActivity() {
        drawer.openDrawer(GravityCompat.START, false)
        assertTrue(drawer.isDrawerOpen(GravityCompat.START))

        activity.onBackPressedDispatcher.onBackPressed()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        drawer.computeScroll()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(drawer.isDrawerVisible(GravityCompat.START))
        assertFalse(activity.isFinishing)
    }

    @Test
    fun backWithClosedDrawerUsesDefaultNavigation() {
        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun backClosesDrawerRestoredBeforeFirstLayout() {
        drawer.openDrawer(GravityCompat.START, false)
        val savedState = SparseArray<Parcelable>()
        drawer.saveHierarchyState(savedState)
        controller.pause().stop().destroy()
        createHost(savedState)
        assertTrue(drawer.isDrawerOpen(GravityCompat.START))

        activity.onBackPressedDispatcher.onBackPressed()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        drawer.computeScroll()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(drawer.isDrawerVisible(GravityCompat.START))
        assertFalse(activity.isFinishing)
    }
}
