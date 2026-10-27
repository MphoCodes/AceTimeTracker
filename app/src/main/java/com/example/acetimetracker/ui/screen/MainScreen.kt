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
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.acetimetracker.R
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.data.repository.TimesheetStats
import com.example.acetimetracker.ui.components.AceCard
import com.example.acetimetracker.ui.components.IconBadge
import com.example.acetimetracker.ui.components.SectionLabel
import com.example.acetimetracker.ui.components.StatTile
import com.example.acetimetracker.ui.components.AceAlertDialog
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.TimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DrawerItem(val title: String, val icon: ImageVector, val destination: Destination)

private val drawerSections = listOf(
    "Track" to listOf(
        DrawerItem("Timer", PhosphorIcons.Regular.Timer, Destination.Timer),
        DrawerItem("Timesheet", PhosphorIcons.Regular.FileText, Destination.Timesheet),
        DrawerItem("Categories", PhosphorIcons.Regular.Folder, Destination.Categories)
    ),
    "Insights" to listOf(
        DrawerItem("Goal progress", PhosphorIcons.Regular.ChartLineUp, Destination.MyProgress),
        DrawerItem("Time breakdown", PhosphorIcons.Regular.ChartPie, Destination.Graph)
    ),
    "Account" to listOf(
        DrawerItem("Profile", PhosphorIcons.Regular.User, Destination.Profile),
        DrawerItem("Settings", PhosphorIcons.Regular.Gear, Destination.Settings),
        DrawerItem("About", PhosphorIcons.Regular.Info, Destination.About)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userProfile: UserProfile?,
    onNavigate: (Destination) -> Unit,
    timesheets: List<TimesheetEntry> = emptyList(),
    categories: List<Category> = emptyList(),
    isLoading: Boolean = false
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val navigateFromDrawer: (Destination) -> Unit = { destination ->
        scope.launch { drawerState.close() }
        if (destination == Destination.Logout) showLogoutDialog = true else onNavigate(destination)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(userProfile = userProfile, onNavigate = navigateFromDrawer)
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    userProfile = userProfile,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onProfileClick = { onNavigate(Destination.Profile) }
                )
            },
            bottomBar = { HomeBottomBar(onNavigate = onNavigate) }
        ) { paddingValues ->
            HomeContent(
                userProfile = userProfile,
                timesheets = timesheets,
                categories = categories,
                isLoading = isLoading,
                onNavigate = onNavigate,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    if (showLogoutDialog) {
        AceAlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(PhosphorIcons.Regular.SignOut, contentDescription = null) },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to see your sessions.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onNavigate(Destination.Logout)
                }) { Text("Log out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    userProfile: UserProfile?,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.acelogo),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(PhosphorIcons.Regular.List, contentDescription = "Open menu", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                }
            },
            actions = {
                IconButton(onClick = onProfileClick) {
                    Avatar(url = userProfile?.profilePictureUrl, size = 34.dp, contentDescription = "Profile")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun HomeBottomBar(onNavigate: (Destination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(PhosphorIcons.Regular.House, contentDescription = null, modifier = Modifier.size(22.dp)) },
            label = { Text("Today") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = false,
            onClick = { onNavigate(Destination.Timer) },
            icon = { Icon(PhosphorIcons.Regular.Timer, contentDescription = null, modifier = Modifier.size(22.dp)) },
            label = { Text("Timer") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = false,
            onClick = { onNavigate(Destination.Timesheet) },
            icon = { Icon(PhosphorIcons.Regular.FileText, contentDescription = null, modifier = Modifier.size(22.dp)) },
            label = { Text("Timesheet") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = false,
            onClick = { onNavigate(Destination.MyProgress) },
            icon = { Icon(PhosphorIcons.Regular.ChartLineUp, contentDescription = null, modifier = Modifier.size(22.dp)) },
            label = { Text("Progress") },
            colors = itemColors
        )
    }
}

@Composable
private fun HomeContent(
    userProfile: UserProfile?,
    timesheets: List<TimesheetEntry>,
    categories: List<Category>,
    isLoading: Boolean,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { TimeUtils.today() }
    val weekStart = remember { TimeUtils.startOfWeek() }
    val todayMillis = remember(timesheets) { TimesheetStats.millisOn(timesheets, today) }
    val weekMillis = remember(timesheets) { TimesheetStats.millisOnOrAfter(timesheets, weekStart) }
    val streak = remember(timesheets) { TimesheetStats.currentStreakDays(timesheets, today) }
    val weekByCategory = remember(timesheets) {
        TimesheetStats.hoursByCategory(timesheets.filter { it.startDate >= weekStart })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AceRed)
        }

        Greeting(name = userProfile?.displayName.orEmpty())

        TodayHeroCard(
            todayMillis = todayMillis,
            onStartTimer = { onNavigate(Destination.Timer) },
            onLogManually = { onNavigate(Destination.Timesheet) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "This week",
                value = TimeUtils.formatDurationShort(weekMillis),
                icon = PhosphorIcons.Regular.CalendarDots,
                accent = AccentBlue,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = if (streak == 1) "Day streak" else "Days in a row",
                value = streak.toString(),
                icon = PhosphorIcons.Regular.Flame,
                accent = AccentAmber,
                modifier = Modifier.weight(1f)
            )
        }

        if (!isLoading && categories.isEmpty()) {
            GetStartedCard(onCreateCategory = { onNavigate(Destination.Categories) })
        } else if (categories.isNotEmpty()) {
            WeeklyGoalsCard(
                categories = categories,
                weekHours = weekByCategory,
                onSeeAll = { onNavigate(Destination.MyProgress) }
            )
        }

        RecentSessionsCard(
            entries = timesheets.take(3),
            categories = categories,
            onSeeAll = { onNavigate(Destination.Timesheet) }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Greeting(name: String) {
    val (greeting, dateText) = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val g = when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        g to SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (name.isNotBlank()) "$greeting, ${name.substringBefore(' ')}" else greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun TodayHeroCard(
    todayMillis: Long,
    onStartTimer: () -> Unit,
    onLogManually: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AceRed
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Tracked today",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = TimeUtils.formatDurationShort(todayMillis),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (todayMillis == 0L) "Nothing logged yet. A short session is a great start." else "Nice work. Keep the momentum going.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartTimer,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AceRedDark)
                ) {
                    Icon(PhosphorIcons.Regular.Play, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start timer", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onLogManually,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(PhosphorIcons.Regular.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log time", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun GetStartedCard(onCreateCategory: () -> Unit) {
    AceCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = PhosphorIcons.Regular.FolderPlus, tint = AceRed)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Set up your first category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Group your time (reading, exercise, a side project) and give it an hour goal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(onClick = onCreateCategory, modifier = Modifier.fillMaxWidth()) {
            Text("Create category")
        }
    }
}

@Composable
private fun CardHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(title, modifier = Modifier.weight(1f))
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all", color = MaterialTheme.colorScheme.primary)
                Icon(PhosphorIcons.Regular.CaretRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WeeklyGoalsCard(
    categories: List<Category>,
    weekHours: Map<String, Float>,
    onSeeAll: () -> Unit
) {
    val rows = remember(categories, weekHours) {
        categories
            .sortedByDescending { weekHours[it.name] ?: 0f }
            .take(4)
    }
    AceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)) {
        CardHeader(title = "This week's goals", onSeeAll = onSeeAll)
        Column(
            modifier = Modifier.padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            rows.forEach { category ->
                CategoryProgressRow(category = category, hours = weekHours[category.name] ?: 0f)
            }
        }
    }
}

@Composable
fun CategoryProgressRow(category: Category, hours: Float, modifier: Modifier = Modifier) {
    val color = parseHexColor(category.colorHex)
    val goal = category.goal?.takeIf { it > 0f }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (goal != null) "${formatHours(hours)} / ${formatHours(goal)}" else formatHours(hours),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (goal != null) (hours / goal).coerceIn(0f, 1f) else if (hours > 0f) 1f else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun RecentSessionsCard(
    entries: List<TimesheetEntry>,
    categories: List<Category>,
    onSeeAll: () -> Unit
) {
    AceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
        CardHeader(title = "Recent sessions", onSeeAll = if (entries.isNotEmpty()) onSeeAll else null)
        if (entries.isEmpty()) {
            Text(
                text = "Your saved sessions will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 12.dp)
            )
        } else {
            entries.forEach { entry ->
                val color = categories.find { it.name == entry.category }?.colorHex?.let { parseHexColor(it) } ?: AceRed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(icon = PhosphorIcons.Regular.Clock, tint = color, size = 36.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${relativeDay(entry.startDate)} · ${entry.startTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = TimeUtils.formatDurationShort(entry.timeSpent),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun relativeDay(date: String): String {
    val today = TimeUtils.today()
    if (date == today) return "Today"
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
    if (date == TimeUtils.formatDate(yesterday)) return "Yesterday"
    return date
}

@Composable
private fun Avatar(url: String?, size: androidx.compose.ui.unit.Dp, contentDescription: String?) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(size)
    ) {
        if (!url.isNullOrEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    PhosphorIcons.Regular.User,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size / 2)
                )
            }
        }
    }
}

@Composable
private fun AppDrawer(
    userProfile: UserProfile?,
    onNavigate: (Destination) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    ) {
        // Header: tap-through to profile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Open profile") { onNavigate(Destination.Profile) }
                .statusBarsPadding()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(url = userProfile?.profilePictureUrl, size = 52.dp, contentDescription = null)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userProfile?.displayName?.ifBlank { null } ?: "Your profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val email = userProfile?.email.orEmpty()
                Text(
                    text = email.ifBlank { "View and edit your details" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(PhosphorIcons.Regular.CaretRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            drawerSections.forEach { (header, items) ->
                SectionLabel(header, modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 6.dp))
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        label = { Text(item.title, fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = { onNavigate(item.destination) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        shape = RoundedCornerShape(14.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        NavigationDrawerItem(
            icon = { Icon(PhosphorIcons.Regular.SignOut, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
            label = { Text("Log out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
            selected = false,
            onClick = { onNavigate(Destination.Logout) },
            modifier = Modifier
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .padding(vertical = 8.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(14.dp)
        )
    }
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

@androidx.compose.ui.tooling.preview.Preview(name = "Today", heightDp = 1100)
@Composable
private fun MainScreenPreview() {
    val today = TimeUtils.today()
    AceTimeTrackerTheme(darkTheme = false) {
        MainScreen(
            userProfile = UserProfile(displayName = "Mpho Ndlela", email = "mpho@example.com"),
            onNavigate = {},
            timesheets = listOf(
                TimesheetEntry(id = "1", category = "Reading", startDate = today, startTime = "08:00", endDate = today, endTime = "09:15", timeSpent = 75 * 60 * 1000L),
                TimesheetEntry(id = "2", category = "Exercise", startDate = today, startTime = "06:30", endDate = today, endTime = "07:00", timeSpent = 30 * 60 * 1000L)
            ),
            categories = listOf(
                Category("a", "Reading", goal = 5f, colorHex = "#2563EB"),
                Category("b", "Exercise", goal = 3f, colorHex = "#10B981")
            )
        )
    }
}
