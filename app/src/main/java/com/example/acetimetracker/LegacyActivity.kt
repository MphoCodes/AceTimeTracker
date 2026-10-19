package com.example.acetimetracker

import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.acetimetracker.core.ui.installLegacyWindowInsets

/** Temporary inset owner for XML screens. The future Compose shell owns its own insets. */
abstract class LegacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Required for IME inset delivery on pre-Android 11 windows.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        installLegacyWindowInsets(findViewById<ViewGroup>(android.R.id.content))
    }
}
