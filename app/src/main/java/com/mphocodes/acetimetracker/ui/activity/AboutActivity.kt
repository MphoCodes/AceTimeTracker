package com.mphocodes.acetimetracker.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mphocodes.acetimetracker.ui.screen.AboutScreen
import com.mphocodes.acetimetracker.ui.theme.AceTimeTrackerTheme

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
