package com.example.acetimetracker.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.acetimetracker.ui.screen.AboutScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme

class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AceTimeTrackerTheme {
                AboutScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}
