package com.example.acetimetracker.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.ui.screen.MyProgressScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.ProgressViewModel

class MyProgressActivity : ComponentActivity() {

    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AceTimeTrackerTheme {
                val categoriesState by viewModel.categoriesState.observeAsState()
                val timesheetsState by viewModel.timesheetsState.observeAsState()

                MyProgressScreen(
                    categoriesState = categoriesState,
                    timesheetsState = timesheetsState,
                    onBackClick = { finish() }
                )
            }
        }

        viewModel.loadCategories()
        viewModel.loadTimesheets()
    }
}
