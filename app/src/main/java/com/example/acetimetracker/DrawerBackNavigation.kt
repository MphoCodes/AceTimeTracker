package com.example.acetimetracker

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.core.view.doOnLayout
import androidx.drawerlayout.widget.DrawerLayout

/** Close the navigation drawer on Back; let the activity handle Back when closed. */
fun ComponentActivity.handleDrawerBack(drawer: DrawerLayout) {
    val callback = object : OnBackPressedCallback(drawer.isDrawerVisible(GravityCompat.START)) {
        override fun handleOnBackPressed() {
            drawer.closeDrawer(GravityCompat.START)
        }
    }
    drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            callback.isEnabled = slideOffset > 0f
        }

        override fun onDrawerOpened(drawerView: View) {
            callback.isEnabled = true
        }

        override fun onDrawerClosed(drawerView: View) {
            callback.isEnabled = false
        }
    })
    // Restoring an open drawer before its first layout does not notify drawer listeners.
    drawer.doOnLayout { callback.isEnabled = drawer.isDrawerVisible(GravityCompat.START) }
    onBackPressedDispatcher.addCallback(this, callback)
}
