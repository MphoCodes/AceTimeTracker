package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, Slate200)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "About AceTimeTracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Slate800
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Slate50)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Authentic AceTimeTracker Logo Display
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                shadowElevation = 1.dp,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.acelogo),
                    contentDescription = "AceTimeTracker Logo",
                    modifier = Modifier
                        .size(130.dp)
                        .padding(16.dp)
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
                        color = Slate900
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = AceRedLight,
                    border = BorderStroke(1.dp, AceRed.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Version 1.0 • Study & Task Management",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = AceRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Authentic Application Purpose & Creator Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AceRedLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = AceRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Built for Students & Learners",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "This application was developed with students in mind. It aims to provide a clean, intuitive, and distraction-free interface for tracking study time and managing academic goals efficiently.\n\nDeveloped by Mpho Ndlela.\nWe hope it helps you study better and ACE IT!!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600,
                        lineHeight = 22.sp
                    )
                }
            }

            // Core Capabilities
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CORE CAPABILITIES",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    modifier = Modifier.padding(start = 4.dp)
                )

                AboutFeatureRow(
                    icon = Icons.Default.DateRange,
                    title = "Timesheet Hours & Proofs",
                    description = "Log your daily task hours with photo evidence attachments.",
                    accentColor = AccentBlue
                )

                AboutFeatureRow(
                    icon = Icons.Default.Timer,
                    title = "Live Stopwatch Timer",
                    description = "Track task sessions with real-time feedback on goal thresholds.",
                    accentColor = AceRed
                )

                AboutFeatureRow(
                    icon = Icons.Default.Folder,
                    title = "Category Goals & Limits",
                    description = "Establish minimum and maximum study target hours per category.",
                    accentColor = AccentAmber
                )

                AboutFeatureRow(
                    icon = Icons.Default.TrendingUp,
                    title = "Goal Progress & Distribution",
                    description = "Monitor actual performance against targets with interactive charts.",
                    accentColor = AccentEmerald
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "© AceTimeTracker • Developed by Mpho Ndlela",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
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
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
