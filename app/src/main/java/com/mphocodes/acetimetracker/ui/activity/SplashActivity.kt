package com.mphocodes.acetimetracker.ui.activity

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mphocodes.acetimetracker.R
import com.mphocodes.acetimetracker.data.repository.AuthRepository
import com.mphocodes.acetimetracker.ui.theme.AceRed
import com.mphocodes.acetimetracker.ui.theme.AceTimeTrackerTheme

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    // Brief brand moment only; long forced waits delay people who just want to start tracking.
    private val splashDelay: Long = 700
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
fun SplashScreen(modifier: Modifier = Modifier) {
    val preview = androidx.compose.ui.platform.LocalInspectionMode.current
    var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(preview) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(420), label = "Brand reveal"
    )
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp).graphicsLayer { this.alpha = alpha; translationY = (1f - alpha) * 12f },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Image(
                    painter = painterResource(R.drawable.ace_mark), contentDescription = null,
                    modifier = Modifier.padding(20.dp).size(112.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Ace", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("TIME TRACKER", style = MaterialTheme.typography.labelLarge, letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Make time for what matters.", style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SplashLightPreview() { AceTimeTrackerTheme(darkTheme = false) { SplashScreen() } }

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SplashDarkPreview() { AceTimeTrackerTheme(darkTheme = true) { SplashScreen() } }
