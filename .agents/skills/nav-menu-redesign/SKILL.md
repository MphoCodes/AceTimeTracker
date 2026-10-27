---
name: nav-menu-redesign
description: Guides the modern redesign and implementation of navigation drawers, bottom navigation bars, and navigation rails in Jetpack Compose Material 3. Use whenever redesigning, optimizing, or modernizing an Android app's navigation architecture, menu drawer layout, or tab bar affordances.
---

# Navigation Menu Redesign Skill

## 1. Architectural Principles
Modern Android navigation in Jetpack Compose emphasizes clarity, ergonomic thumb reachability, and clean visual hierarchy following Material Design 3 guidelines:
1. **Logical Categorization**: Never present a flat, unsegmented list of more than 5 items. Group navigation destinations into clear semantic sections (e.g., *Tracking & Actions*, *Insights & Analytics*, *Preferences & Account*).
2. **Distinct Active Indicators**: Use `NavigationDrawerItem` or `NavigationBarItem` with custom `NavigationDrawerItemDefaults.colors()` or container shapes (`RoundedCornerShape` pill) so the user's current destination is immediately discernible.
3. **Ergonomic Drawer Header**:
   - Integrated user profile preview (avatar, display name, email).
   - Brand aesthetic continuity (consistent signature gradients or brand color backdrops).
   - Tap-through directly to account/profile settings.
4. **Safe Insets & Edge-to-Edge**: Always apply `WindowInsets.statusBars` and `WindowInsets.navigationBars` to avoid system gesture overlaps.
5. **Secondary Affordances & Counters**: Provide badge indicators on high-activity destinations (e.g., active timers, number of recorded categories, pending goals).

---

## 2. Jetpack Compose Implementation Pattern

### 2.1 Navigation Item Data Structure
```kotlin
data class NavMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int? = null,
    val badgeText: String? = null,
    val destination: Destination,
    val tintColor: Color? = null
)

data class NavMenuSection(
    val header: String? = null,
    val items: List<NavMenuItem>
)
```

### 2.2 Modern Drawer Header
```kotlin
@Composable
fun ModernDrawerHeader(
    userProfile: UserProfile?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .clickable(onClick = onProfileClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(60.dp)
            ) {
                if (userProfile?.profilePictureUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = userProfile.profilePictureUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AceRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userProfile?.displayName?.ifEmpty { "Ace Tracker User" } ?: "Ace Tracker User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = userProfile?.email?.ifEmpty { "user@acetimetracker.com" } ?: "user@acetimetracker.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

### 2.3 Sectioned Drawer Content
```kotlin
@Composable
fun ModernDrawerContent(
    sections: List<NavMenuSection>,
    currentDestination: Destination,
    onNavigate: (Destination) -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            sections.forEach { section ->
                section.header?.let { headerText ->
                    item {
                        Text(
                            text = headerText.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }

                items(section.items) { item ->
                    val isSelected = currentDestination == item.destination
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else (item.tintColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        badge = {
                            if (item.badgeText != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = item.badgeText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = { onNavigate(item.destination) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    Divider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = AceRed) },
            label = { Text("Log Out", color = AceRed, fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = onLogoutClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
```

---

## 3. Review Checklist
- [ ] Drawer width is standard 280dp-320dp on mobile.
- [ ] Insets (statusBars & navigationBars) are padded properly.
- [ ] Active destination pill clearly contrasts with resting items.
- [ ] Visual categorization clarifies destinations instead of one long unstructured list.
- [ ] Destructive actions (Logout, Delete) are separated by a divider and colored with error tint.
- [ ] Touch targets are at least 48dp tall for ergonomic accessibility.
