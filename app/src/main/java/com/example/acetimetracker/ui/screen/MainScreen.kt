package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.ChartPie
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.TrendingUp
import com.composables.icons.lucide.User
import com.example.acetimetracker.R
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
            ) {
                // Authentic AceTimeTracker Drawer Header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Profile)
                        },
                    color = Slate900
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // User Avatar or App Logo
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(2.dp, AceRed),
                                modifier = Modifier.size(54.dp)
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
                                    Image(
                                        painter = painterResource(id = R.drawable.acelogo),
                                        contentDescription = "AceTimeTracker Logo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile?.displayName?.ifEmpty { "Ace Tracker User" } ?: "Ace Tracker User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userProfile?.email?.ifEmpty { "st10203065@vcconnect.edu.za" } ?: "st10203065@vcconnect.edu.za",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate800
                        ) {
                            Text(
                                text = "Ace It! • Student & Task Productivity",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate300,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Drawer Navigation Items with Lucide Icons
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "CORE FEATURES",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    DrawerNavRow(
                        title = "Timesheet Log",
                        icon = Lucide.FileText,
                        badge = "Log",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Timesheet)
                        }
                    )

                    DrawerNavRow(
                        title = "Stopwatch Timer",
                        icon = Lucide.Timer,
                        badge = "Live",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Timer)
                        }
                    )

                    DrawerNavRow(
                        title = "Task Categories",
                        icon = Lucide.Folder,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Categories)
                        }
                    )

                    DrawerNavRow(
                        title = "My Goal Progress",
                        icon = Lucide.TrendingUp,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.MyProgress)
                        }
                    )

                    DrawerNavRow(
                        title = "Analytics Graph",
                        icon = Lucide.ChartPie,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Graph)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = Slate100
                    )

                    Text(
                        text = "ACCOUNT & APP",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    DrawerNavRow(
                        title = "User Profile",
                        icon = Lucide.User,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Profile)
                        }
                    )

                    DrawerNavRow(
                        title = "Settings",
                        icon = Lucide.Settings,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.Settings)
                        }
                    )

                    DrawerNavRow(
                        title = "About AceTimeTracker",
                        icon = Lucide.Info,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Destination.About)
                        }
                    )
                }

                // Log Out Action
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Lucide.LogOut,
                            contentDescription = "Log Out",
                            tint = AceRed,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            "Log Out",
                            color = AceRed,
                            fontWeight = FontWeight.SemiBold
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

                // Author Stamp
                Text(
                    text = "AceTimeTracker • Developed by Mpho Ndlela",
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
                // TopAppBar featuring authentic R.drawable.acelogo
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, Slate200)
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.acelogo),
                                    contentDescription = "AceTimeTracker Logo",
                                    modifier = Modifier.size(34.dp)
                                )
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
                                Icon(Lucide.Menu, contentDescription = "Menu", tint = Slate800, modifier = Modifier.size(22.dp))
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
                                            Lucide.User,
                                            contentDescription = "Profile",
                                            tint = Slate700,
                                            modifier = Modifier.size(18.dp)
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
                // Native Clean Bottom Navigation Bar with Lucide Icons
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
                            icon = { Icon(Lucide.House, contentDescription = "Home", modifier = Modifier.size(22.dp)) },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AceRed,
                                selectedTextColor = AceRed,
                                indicatorColor = AceRedLight.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { onNavigate(Destination.Timesheet) },
                            icon = { Icon(Lucide.FileText, contentDescription = "Timesheet", modifier = Modifier.size(22.dp)) },
                            label = { Text("Timesheet", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AceRed,
                                selectedTextColor = AceRed,
                                indicatorColor = AceRedLight.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { onNavigate(Destination.Timer) },
                            icon = { Icon(Lucide.Timer, contentDescription = "Stopwatch", modifier = Modifier.size(22.dp)) },
                            label = { Text("Stopwatch", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AceRed,
                                selectedTextColor = AceRed,
                                indicatorColor = AceRedLight.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { onNavigate(Destination.MyProgress) },
                            icon = { Icon(Lucide.TrendingUp, contentDescription = "Progress", modifier = Modifier.size(22.dp)) },
                            label = { Text("Progress", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
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
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Entry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            floatingActionButtonPosition = FabPosition.Center
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
                // Authentic AceTimeTracker Welcome Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.acelogo),
                                    contentDescription = "Ace Logo",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACETIMETRACKER",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Slate300
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = AceRed.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, AceRed.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Study & ACE IT!",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                        Text(
                            text = userProfile?.displayName?.ifEmpty { "Ace Learner" } ?: "Ace Learner",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Track your study hours, stay on top of category goals, and balance your daily tasks with ease.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate300,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Real Functional Action Buttons
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
                                Icon(Lucide.Play, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Hours", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Section: App Features
                Text(
                    text = "APP FEATURES",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )

                // 2x2 Grid of Real Feature Cards with Lucide Icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Timesheet Log",
                        description = "Log hours & photo proofs",
                        icon = Lucide.FileText,
                        accentColor = AccentBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Timesheet) }
                    )
                    FeatureCard(
                        title = "Stopwatch Timer",
                        description = "Real-time task tracking",
                        icon = Lucide.Timer,
                        accentColor = AceRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Timer) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Task Categories",
                        description = "Set min/max hour goals",
                        icon = Lucide.Folder,
                        accentColor = AccentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.Categories) }
                    )
                    FeatureCard(
                        title = "Goal Progress",
                        description = "Min vs max target status",
                        icon = Lucide.TrendingUp,
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Destination.MyProgress) }
                    )
                }

                FeatureCard(
                    title = "Visual Analytics & Distribution",
                    description = "Interactive pie chart breakdown of your task time distribution",
                    icon = Lucide.ChartPie,
                    accentColor = AccentIndigo,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate(Destination.Graph) }
                )

                // Helpful Study Tips Card with Lucide.Lightbulb
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AceRedLight,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Lucide.Lightbulb,
                                    contentDescription = null,
                                    tint = AceRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Pro Tip: Set Goal Boundaries",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Configure minimum and maximum target hours under Task Categories to keep your work-life balance on track.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun DrawerNavRow(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Slate600,
                modifier = Modifier.size(20.dp)
            )
        },
        label = {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate800
            )
        },
        badge = {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AceRedLight
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AceRed
                    )
                }
            }
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(44.dp),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
fun FeatureCard(
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
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class BottomNavTab {
    Home,
    Timesheet,
    Timer,
    Progress
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
