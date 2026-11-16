package com.mphocodes.acetimetracker.ui.screen

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import com.mphocodes.acetimetracker.reminder.ReminderSchedule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mphocodes.acetimetracker.data.repository.SettingsRepository
import com.mphocodes.acetimetracker.ui.components.AceCard
import com.mphocodes.acetimetracker.ui.components.AceTopBar
import com.mphocodes.acetimetracker.ui.components.IconBadge
import com.mphocodes.acetimetracker.ui.components.SectionLabel
import com.mphocodes.acetimetracker.ui.theme.*

private data class ThemeOption(val value: String, val label: String, val description: String, val icon: ImageVector)

private val themeOptions = listOf(
    ThemeOption(SettingsRepository.THEME_SYSTEM, "System default", "Match your device setting", PhosphorIcons.Regular.DeviceMobile),
    ThemeOption(SettingsRepository.THEME_LIGHT, "Light", "Bright surfaces, dark text", PhosphorIcons.Regular.Sun),
    ThemeOption(SettingsRepository.THEME_DARK, "Dark", "Easier on the eyes at night", PhosphorIcons.Regular.Moon)
)

/** Preferences are applied as soon as they change, so there is no separate save step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    initialNotificationsEnabled: Boolean,
    onBackClick: () -> Unit,
    onThemeChange: (String) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    reminder: ReminderSchedule = ReminderSchedule(),
    notificationsAllowed: Boolean = true,
    onReminderChange: (ReminderSchedule) -> Unit = {},
    onReminderTimeClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {}
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
                    IconBadge(icon = PhosphorIcons.Regular.Bell, tint = AceRed, size = 36.dp)
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
            SectionLabel("Daily reminder")
            AceCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = reminder.enabled, role = Role.Switch,
                            onValueChange = { onReminderChange(reminder.copy(enabled = it)) }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Remember to track", style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold)
                            Text("An optional nudge on the days you choose.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = reminder.enabled, onCheckedChange = null)
                    }
                    OutlinedButton(onClick = onReminderTimeClick) {
                        Text("Time: " + LocalTime.of(reminder.hour, reminder.minute)
                            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.values().forEach { day ->
                                FilterChip(
                                    selected = day in reminder.days,
                                    onClick = {
                                        val days = if (day in reminder.days) reminder.days - day else reminder.days + day
                                        onReminderChange(reminder.copy(days = days, enabled = reminder.enabled && days.isNotEmpty()))
                                    },
                                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                                )
                        }
                    }
                    if (reminder.days.isEmpty()) {
                        Text("Select at least one day to enable reminders.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Delivery is approximate and follows your device time zone. Battery settings may delay it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!notificationsAllowed) {
                        Text("Notifications are blocked. Tracking still works; allow notifications to receive reminders.",
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onNotificationSettingsClick) {
                            Text("Open notification settings")
                        }
                    }
                }
            }
        }
    }
}
