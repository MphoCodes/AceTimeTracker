package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userProfile: UserProfile?,
    onNavigate: (Destination) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedBottomTab by remember { mutableStateOf(BottomNavTab.Home) }

    val currentDateStr = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    // Curated Navigation Drawer Categories
    val navSections = remember {
        listOf(
            NavSection(
                title = "Productivity Hub",
                items = listOf(
                    NavMenuItem("Timesheet", "Log Timesheet", Icons.Default.DateRange, Destination.Timesheet, badge = "Add"),
                    NavMenuItem("Timer", "Productivity Stopwatch", Icons.Default.PlayArrow, Destination.Timer, badge = "Live"),
                    NavMenuItem("Categories", "Task Categories", Icons.Default.Folder, Destination.Categories)
                )
            ),
            NavSection(
                title = "Reports & Analytics",
                items = listOf(
                    NavMenuItem("Progress", "Goals & Milestones", Icons.Default.TrendingUp, Destination.MyProgress),
                    NavMenuItem("Analytics", "Visual Distribution", Icons.Default.BarChart, Destination.Graph)
                )
            ),
            NavSection(
                title = "Account & System",
                items = listOf(
                    NavMenuItem("Profile", "My Profile", Icons.Default.Person, Destination.Profile),
                    NavMenuItem("Settings", "App Preferences", Icons.Default.Settings, Destination.Settings),
                    NavMenuItem("About", "About AceTimeTracker", Icons.Default.Info, Destination.About)
                )
            )
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                // Sleek Executive Profile Header with Gradient Backdrop
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Slate900, Slate800)
                            )
                        )
                        .statusBarsPadding()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Profile)
                        }
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Surface(
                                    shape = CircleShape,
                                    color = Slate700,
                                    border = BorderStroke(2.dp, AceRed),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    if (userProfile?.profilePictureUrl?.isNotEmpty() == true) {
                                        AsyncImage(
                                            model = userProfile.profilePictureUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                // Online Status Dot
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .background(AccentEmerald, CircleShape)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile?.displayName?.ifEmpty { "Ace Tracker" } ?: "Ace Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userProfile?.email?.ifEmpty { "user@acetimetracker.com" } ?: "user@acetimetracker.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Executive Status Pill
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Slate800,
                            border = BorderStroke(1.dp, Slate700)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(AceRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PRO WORKSPACE • ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 0.8.sp,
                                        fontSize = 10.sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Slate300
                                )
                            }
                        }
                    }
                }

                // Sectioned Drawer List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    navSections.forEach { section ->
                        item {
                            Text(
                                text = section.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        items(section.items) { item ->
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = Slate600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate800
                                    )
                                },
                                badge = {
                                    if (item.badge != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AceRedLight
                                        ) {
                                            Text(
                                                text = item.badge,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AceRed
                                            )
                                        }
                                    }
                                },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onNavigate(item.destination)
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                color = Slate100
                            )
                        }
                    }
                }

                // Log Out Action
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Log Out",
                            tint = AceRed,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            "Log Out",
                            color = AceRed,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Destination.Logout)
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                // Version Branding
                Text(
                    text = "AceTimeTracker • Executive v2.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                // High-End Surface TopAppBar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, Slate200)
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AceRed,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "A",
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Ace",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = AceRed
                                        )
                                        Text(
                                            text = "TimeTracker",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                    }
                                    Text(
                                        text = currentDateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Slate800)
                            }
                        },
                        actions = {
                            IconButton(onClick = { onNavigate(Destination.Profile) }) {
                                Surface(
                                    shape = CircleShape,
                                    color = Slate100,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = Slate700,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                // Minimalist, Clean Bottom Navigation Bar with Proper Insets
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, Slate200)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = selectedBottomTab == BottomNavTab.Home,
                            onClick = { selectedBottomTab = BottomNavTab.Home },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AceRed,
                                selectedTextColor = AceRed,
                                indicatorColor = AceRedLight.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedBottomTab == BottomNavTab.Timesheet,
                            onClick = { onNavigate(Destination.Timesheet) },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Timesheet") },
                            label = { Text("Timesheet", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AceRed,
                                selectedTextColor = AceRed,
                                indicatorColor = AceRedLight.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedBottomTab == BottomNavTab.Timer,
                            onClick = { onNavigate(Destination.Timer) },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Timer") },
                            label = { Text("Stopwatch", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AceRed,
                                selectedTextColor = AceRed,
                                indicatorColor = AceRedLight.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedBottomTab == BottomNavTab.Categories,
                            onClick = { onNavigate(Destination.Graph) },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                            label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AceRed,
                                selectedTextColor = AceRed,
                                indicatorColor = AceRedLight.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { onNavigate(Destination.Timesheet) },
                    containerColor = AceRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Time", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Slate50)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Executive Performance & Productivity Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AccentEmerald, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TODAY'S PRODUCTIVITY",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Slate300
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = AccentEmerald.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "On Track • 68%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Metric Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "04",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "h ",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate400
                                    )
                                    Text(
                                        text = "30",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "m",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate400
                                    )
                                }
                                Text(
                                    text = "Daily Target: 8h 00m • 3h 30m remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }

                            // Circular or Pill Mini Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate800,
                                border = BorderStroke(1.dp, Slate700)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "28.5 hrs",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "This Week",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { 0.68f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentEmerald,
                            trackColor = Slate800,
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Quick Control Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onNavigate(Destination.Timer) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AceRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Timer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { onNavigate(Destination.Timesheet) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Slate700),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Entry", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Section: Productivity Workspace Hub
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WORKSPACE HUBS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Text(
                        text = "All Tools",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AceRed,
                        modifier = Modifier.clickable { scope.launch { drawerState.open() } }
                    )
                }

                // 2x2 Grid of Elegant Executive Action Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExecutiveActionCard(
                        title = "Timesheet Log",
                        description = "Review entries & photos",
                        icon = Icons.Default.DateRange,
                        accentColor = AccentBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Timesheet) }
                    )
                    ExecutiveActionCard(
                        title = "Task Categories",
                        description = "Organize goals & limits",
                        icon = Icons.Default.Folder,
                        accentColor = AccentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Categories) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExecutiveActionCard(
                        title = "Goal Milestones",
                        description = "Min vs max target limits",
                        icon = Icons.Default.TrendingUp,
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.MyProgress) }
                    )
                    ExecutiveActionCard(
                        title = "Visual Analytics",
                        description = "Time distribution charts",
                        icon = Icons.Default.PieChart,
                        accentColor = AccentIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Graph) }
                    )
                }

                // Section: Recent Activity / Sessions (Gives high-end real feel)
                Text(
                    text = "RECENT TRACKING SESSIONS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RecentSessionRow(
                            title = "Client App Architecture",
                            category = "Engineering",
                            duration = "2h 15m",
                            badgeColor = AccentBlue,
                            onClick = { onNavigate(Destination.Timesheet) }
                        )
                        Divider(color = Slate100)
                        RecentSessionRow(
                            title = "Sprint Planning & Sync",
                            category = "Management",
                            duration = "45m",
                            badgeColor = AccentAmber,
                            onClick = { onNavigate(Destination.Timesheet) }
                        )
                        Divider(color = Slate100)
                        RecentSessionRow(
                            title = "UX Navigation Redesign",
                            category = "Design",
                            duration = "1h 30m",
                            badgeColor = AccentEmerald,
                            onClick = { onNavigate(Destination.Timesheet) }
                        )
                    }
                }

                // Generous bottom spacer so FAB never obscures any content
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun RecentSessionRow(
    title: String,
    category: String,
    duration: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(badgeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Slate100
        ) {
            Text(
                text = duration,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )
        }
    }
}

@Composable
fun ExecutiveActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class NavSection(
    val title: String,
    val items: List<NavMenuItem>
)

data class NavMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val destination: Destination,
    val badge: String? = null
)

enum class BottomNavTab {
    Home,
    Timesheet,
    Timer,
    Categories
}

enum class Destination {
    Profile,
    Categories,
    Timesheet,
    Timer,
    MyProgress,
    Graph,
    Settings,
    About,
    Logout
}
