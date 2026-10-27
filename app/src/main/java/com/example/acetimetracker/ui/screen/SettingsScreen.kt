package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Sun
import com.example.acetimetracker.data.repository.SettingsRepository
import com.example.acetimetracker.ui.components.AceCard
import com.example.acetimetracker.ui.components.AceTopBar
import com.example.acetimetracker.ui.components.IconBadge
import com.example.acetimetracker.ui.components.SectionLabel
import com.example.acetimetracker.ui.theme.*

private data class ThemeOption(val value: String, val label: String, val description: String, val icon: ImageVector)

private val themeOptions = listOf(
    ThemeOption(SettingsRepository.THEME_SYSTEM, "System default", "Match your device setting", Lucide.Smartphone),
    ThemeOption(SettingsRepository.THEME_LIGHT, "Light", "Bright surfaces, dark text", Lucide.Sun),
    ThemeOption(SettingsRepository.THEME_DARK, "Dark", "Easier on the eyes at night", Lucide.Moon)
)

/** Preferences are applied as soon as they change, so there is no separate save step. */
@Composable
fun SettingsScreen(
    currentTheme: String,
    initialNotificationsEnabled: Boolean,
    onBackClick: () -> Unit,
    onThemeChange: (String) -> Unit,
    onNotificationsChange: (Boolean) -> Unit
) {
    var selectedTheme by rememberSaveable { mutableStateOf(currentTheme) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(initialNotificationsEnabled) }

    Scaffold(
        topBar = { AceTopBar(title = "Settings", onBackClick = onBackClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Appearance")
            AceCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                Column(modifier = Modifier.selectableGroup()) {
                    themeOptions.forEachIndexed { index, option ->
                        val selected = selectedTheme == option.value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        selectedTheme = option.value
                                        onThemeChange(option.value)
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(icon = option.icon, tint = AceRed, size = 36.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = AceRed)
                            )
                        }
                        if (index < themeOptions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 66.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            SectionLabel("Notifications")
            AceCard(contentPadding = PaddingValues(0.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .toggleable(
                            value = notificationsEnabled,
                            role = Role.Switch,
                            onValueChange = {
                                notificationsEnabled = it
                                onNotificationsChange(it)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(icon = Lucide.Bell, tint = AceRed, size = 36.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live timer notification", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Show the running stopwatch or focus timer in the notification shade",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = AceRed)
                    )
                }
            }
        }
    }
}
