package com.example.acetimetracker.ui.screen

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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.acetimetracker.R
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userProfile: UserProfile?,
    onNavigate: (Destination) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedBottomTab by remember { mutableStateOf(BottomNavTab.Home) }

    // Navigation Menu Structure (Categorized according to nav-menu-redesign skill)
    val navSections = remember {
        listOf(
            NavSection(
                title = "Track & Log",
                items = listOf(
                    NavMenuItem("Timesheet", "Timesheet", Icons.Default.DateRange, Destination.Timesheet, AceDarkBlue, badgeText = "Log"),
                    NavMenuItem("Timer", "Productivity Stopwatch", Icons.Default.PlayArrow, Destination.Timer, AceRed, badgeText = "Live"),
                    NavMenuItem("Categories", "Task Categories", Icons.Default.Folder, Destination.Categories, AceOrange)
                )
            ),
            NavSection(
                title = "Analytics & Goals",
                items = listOf(
                    NavMenuItem("Progress", "My Progress", Icons.Default.TrendingUp, Destination.MyProgress, AceDarkGreen),
                    NavMenuItem("Analytics", "Visual Analytics", Icons.Default.BarChart, Destination.Graph, AceYellow)
                )
            ),
            NavSection(
                title = "Account & System",
                items = listOf(
                    NavMenuItem("Profile", "User Profile", Icons.Default.Person, Destination.Profile, AceBlue),
                    NavMenuItem("Settings", "App Settings", Icons.Default.Settings, Destination.Settings, AceDarkOrange),
                    NavMenuItem("About", "About AceTimeTracker", Icons.Default.Info, Destination.About, AceBlue)
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
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Ergonomic Drawer Header with Profile Card & Signature Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    DrawerGradientStart,
                                    DrawerGradientCenter,
                                    DrawerGradientEnd
                                )
                            )
                        )
                        .clickable {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Profile)
                        }
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(62.dp)
                        ) {
                            if (userProfile?.profilePictureUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = userProfile.profilePictureUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = AceRed,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userProfile?.displayName?.isNotEmpty() == true) userProfile.displayName else "Ace Tracker User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (userProfile?.email?.isNotEmpty() == true) userProfile.email else "user@acetimetracker.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "View Profile →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Categorized Navigation List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    navSections.forEach { section ->
                        item {
                            Text(
                                text = section.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        items(section.items) { item ->
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.tintColor
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                badge = {
                                    if (item.badgeText != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AceRedLight.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = item.badgeText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent
                                )
                            )
                        }

                        item {
                            Divider(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Destructive Logout Footer
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Log Out",
                            tint = AceRed
                        )
                    },
                    label = {
                        Text(
                            "Log Out",
                            color = AceRed,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Destination.Logout)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Version & Copyright Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "AceTimeTracker v1.0.0 • Material 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "AceTimeTracker",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigate(Destination.Profile) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AceRed,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                // Sleek Material 3 Bottom Navigation Bar for Ergonomic Thumb Reachability
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = selectedBottomTab == BottomNavTab.Home,
                        onClick = { selectedBottomTab = BottomNavTab.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AceRed,
                            selectedTextColor = AceRed,
                            indicatorColor = AceRedLight.copy(alpha = 0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == BottomNavTab.Timesheet,
                        onClick = { onNavigate(Destination.Timesheet) },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Timesheet") },
                        label = { Text("Timesheet") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AceRed,
                            selectedTextColor = AceRed,
                            indicatorColor = AceRedLight.copy(alpha = 0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == BottomNavTab.Timer,
                        onClick = { onNavigate(Destination.Timer) },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Timer") },
                        label = { Text("Timer") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AceRed,
                            selectedTextColor = AceRed,
                            indicatorColor = AceRedLight.copy(alpha = 0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == BottomNavTab.Categories,
                        onClick = { onNavigate(Destination.Categories) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Categories") },
                        label = { Text("Categories") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AceRed,
                            selectedTextColor = AceRed,
                            indicatorColor = AceRedLight.copy(alpha = 0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { scope.launch { drawerState.open() } },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = { Text("More") }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigate(Destination.Timesheet) },
                    containerColor = AceRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Timesheet Entry")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Welcome Banner in Ace Red tint
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AceRedLight.copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (userProfile?.displayName?.isNotEmpty() == true) userProfile.displayName else "Ace Tracker!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AceRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Track every minute with speed and precision.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Authentic Running Dino Lottie Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            LottieAnimationView(context).apply {
                                setAnimation(R.raw.runningdino)
                                repeatCount = LottieDrawable.INFINITE
                                repeatMode = LottieDrawable.RESTART
                                playAnimation()
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                    )
                }

                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vibrant Action Grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    VibrantQuickActionCard(
                        title = "Timesheet",
                        subtitle = "Log & view hours",
                        icon = Icons.Default.DateRange,
                        badgeColor = AceBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Timesheet) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    VibrantQuickActionCard(
                        title = "Timer",
                        subtitle = "Track real-time",
                        icon = Icons.Default.PlayArrow,
                        badgeColor = AceRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Timer) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    VibrantQuickActionCard(
                        title = "Categories",
                        subtitle = "Organize tasks",
                        icon = Icons.Default.Folder,
                        badgeColor = AceOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Categories) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    VibrantQuickActionCard(
                        title = "Analytics",
                        subtitle = "Visual charts",
                        icon = Icons.Default.BarChart,
                        badgeColor = AceGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Graph) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    VibrantQuickActionCard(
                        title = "My Progress",
                        subtitle = "Goal tracking",
                        icon = Icons.Default.TrendingUp,
                        badgeColor = AceDarkGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.MyProgress) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    VibrantQuickActionCard(
                        title = "Settings",
                        subtitle = "App preferences",
                        icon = Icons.Default.Settings,
                        badgeColor = AceDarkOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Settings) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun VibrantQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = badgeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val tintColor: Color,
    val badgeText: String? = null
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
