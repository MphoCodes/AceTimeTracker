package com.example.acetimetracker.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.ui.screen.GraphScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.ProgressViewModel

class GraphActivity : ComponentActivity() {

    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AceTimeTrackerTheme {
                val chartDataState by viewModel.chartDataState.observeAsState()
                val categoriesState by viewModel.categoriesState.observeAsState()

                GraphScreen(
                    chartDataState = chartDataState,
                    categoriesState = categoriesState,
                    onBackClick = { finish() }
                )
            }
        }

        viewModel.loadChartData()
        viewModel.loadCategories()
    }
}
