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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.acetimetracker.data.model.UserProfile
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
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
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
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (userProfile?.displayName?.isNotEmpty() == true) userProfile.displayName else "Ace! Time Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (userProfile?.email?.isNotEmpty() == true) userProfile.email else "user@acetimetracker.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                val navItems = listOf(
                    DrawerItem("Profile", Icons.Default.Person, Destination.Profile),
                    DrawerItem("Categories", Icons.Default.Folder, Destination.Categories),
                    DrawerItem("Timesheet", Icons.Default.DateRange, Destination.Timesheet),
                    DrawerItem("Timer / Stopwatch", Icons.Default.PlayArrow, Destination.Timer),
                    DrawerItem("My Progress", Icons.Default.TrendingUp, Destination.MyProgress),
                    DrawerItem("Analytics Graph", Icons.Default.BarChart, Destination.Graph),
                    DrawerItem("Settings", Icons.Default.Settings, Destination.Settings),
                    DrawerItem("About", Icons.Default.Info, Destination.About)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    navItems.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
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
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Log Out") },
                    label = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
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
                TopAppBar(
                    title = {
                        Text(
                            text = "AceTimeTracker",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigate(Destination.Profile) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigate(Destination.Timesheet) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
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
                // Welcome Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Keep up your productivity goals today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        title = "Timesheet",
                        subtitle = "Log & view hours",
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Timesheet) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    QuickActionCard(
                        title = "Timer",
                        subtitle = "Track real-time",
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Timer) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        title = "Categories",
                        subtitle = "Organize tasks",
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Categories) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    QuickActionCard(
                        title = "Analytics",
                        subtitle = "Visual charts",
                        icon = Icons.Default.BarChart,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Graph) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        title = "My Progress",
                        subtitle = "Goal tracking",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.MyProgress) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    QuickActionCard(
                        title = "Settings",
                        subtitle = "App preferences",
                        icon = Icons.Default.Settings,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Settings) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
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
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
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
    val destination: Destination
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
