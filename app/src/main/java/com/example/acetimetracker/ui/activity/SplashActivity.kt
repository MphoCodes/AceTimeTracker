package com.example.acetimetracker.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acetimetracker.R
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.ui.theme.AceRed
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val splashDelay: Long = 1800
    private val handler = Handler(Looper.getMainLooper())
    private val authRepository = AuthRepository()

    private val runnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            val destination = if (authRepository.isAuthenticated) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AceTimeTrackerTheme {
                SplashScreen()
            }
        }

        handler.postDelayed(runnable, splashDelay)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.acelogo),
                contentDescription = "Ace Logo",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "AceTimeTracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AceRed
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Track Your Time with Precision",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
