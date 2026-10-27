package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChartPie
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Target
import com.composables.icons.lucide.Timer
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.components.AceCard
import com.example.acetimetracker.ui.components.AceTopBar
import com.example.acetimetracker.ui.components.IconBadge
import com.example.acetimetracker.ui.components.SectionLabel
import com.example.acetimetracker.ui.theme.*

@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    Scaffold(
        topBar = { AceTopBar(title = "About", onBackClick = onBackClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.acelogo),
                    contentDescription = "AceTimeTracker logo",
                    modifier = Modifier
                        .size(112.dp)
                        .padding(14.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ace",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = AceRed
                    )
                    Text(
                        text = "TimeTracker",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Lucide.Target, tint = AceRed, size = 36.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Small sessions, real progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AceTimeTracker helps you turn personal goals into steady, repeatable effort. " +
                        "Whether it's studying, reading, training or a side project, start a timer or log a session, " +
                        "then watch your hours add up against the goals you set.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionLabel("What you can do")

                AboutFeatureRow(
                    icon = Lucide.Timer,
                    title = "Stopwatch & focus timer",
                    description = "Track live, or use Pomodoro rounds with built-in breaks.",
                    accentColor = AceRed
                )
                AboutFeatureRow(
                    icon = Lucide.FileText,
                    title = "Timesheet",
                    description = "Log past sessions with notes and an optional photo, then export them.",
                    accentColor = AccentBlue
                )
                AboutFeatureRow(
                    icon = Lucide.Folder,
                    title = "Categories & goals",
                    description = "Group your time and set target, minimum and maximum hours.",
                    accentColor = AccentAmber
                )
                AboutFeatureRow(
                    icon = Lucide.ChartPie,
                    title = "Progress insights",
                    description = "See weekly goal progress, streaks and where your time goes.",
                    accentColor = AccentEmerald
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Made by Mpho Ndlela",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AboutFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AceCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, tint = accentColor, size = 40.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Light")
@Composable
private fun AboutScreenPreview() {
    AceTimeTrackerTheme(darkTheme = false) { AboutScreen(onBackClick = {}) }
}

@Preview(name = "Dark")
@Composable
private fun AboutScreenDarkPreview() {
    AceTimeTrackerTheme(darkTheme = true) { AboutScreen(onBackClick = {}) }
}
