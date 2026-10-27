package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Signature Side Nav Gradient Header (matching side_nav_bar.xml)
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
                        .padding(24.dp)
                ) {
                    Column {
                        if (userProfile?.profilePictureUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = userProfile.profilePictureUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = AceRed,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (userProfile?.displayName?.isNotEmpty() == true) userProfile.displayName else "Ace! Time Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (userProfile?.email?.isNotEmpty() == true) userProfile.email else "user@acetimetracker.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                val navItems = listOf(
                    DrawerItem("Profile", Icons.Default.Person, Destination.Profile, AceBlue),
                    DrawerItem("Categories", Icons.Default.Folder, Destination.Categories, AceOrange),
                    DrawerItem("Timesheet", Icons.Default.DateRange, Destination.Timesheet, AceDarkBlue),
                    DrawerItem("Timer / Stopwatch", Icons.Default.PlayArrow, Destination.Timer, AceRed),
                    DrawerItem("My Progress", Icons.Default.TrendingUp, Destination.MyProgress, AceDarkGreen),
                    DrawerItem("Analytics Graph", Icons.Default.BarChart, Destination.Graph, AceYellow),
                    DrawerItem("Settings", Icons.Default.Settings, Destination.Settings, AceDarkOrange),
                    DrawerItem("About", Icons.Default.Info, Destination.About, AceBlue)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    navItems.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = item.label, tint = item.tint) },
                            label = { Text(item.label, fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigate(item.destination)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = AceRed) },
                    label = { Text("Log Out", color = AceRed, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Destination.Logout)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Classic Bold Ace Red App Bar
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

data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val destination: Destination,
    val tint: Color = Color.Unspecified
)

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
