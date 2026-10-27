package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Target
import com.composables.icons.lucide.TrendingUp
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.data.repository.TimesheetStats
import com.example.acetimetracker.ui.components.AceCard
import com.example.acetimetracker.ui.components.AceTopBar
import com.example.acetimetracker.ui.components.EmptyState
import com.example.acetimetracker.ui.components.SectionLabel
import com.example.acetimetracker.ui.components.StatTile
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils

enum class ProgressPeriod(val label: String) {
    WEEK("This week"),
    MONTH("This month"),
    ALL("All time")
}

@Composable
fun MyProgressScreen(
    categoriesState: Resource<List<Category>>?,
    timesheetsState: Resource<List<TimesheetEntry>>?,
    onBackClick: () -> Unit
) {
    var period by rememberSaveable { mutableStateOf(ProgressPeriod.WEEK) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()
    val allTimesheets = (timesheetsState as? Resource.Success)?.data ?: emptyList()
    val isLoading = timesheetsState is Resource.Loading || categoriesState is Resource.Loading

    val inPeriod = remember(allTimesheets, period) {
        val from = when (period) {
            ProgressPeriod.WEEK -> TimeUtils.startOfWeek()
            ProgressPeriod.MONTH -> TimeUtils.today().take(7) + "-01"
            ProgressPeriod.ALL -> ""
        }
        allTimesheets.filter { it.startDate >= from }
    }
    val hoursByCategory = remember(inPeriod) { TimesheetStats.hoursByCategory(inPeriod) }
    val totalMillis = remember(inPeriod) { inPeriod.sumOf { it.timeSpent.coerceAtLeast(0L) } }
    val goalsMet = categories.count { cat ->
        val goal = cat.goal?.takeIf { it > 0f } ?: return@count false
        (hoursByCategory[cat.name] ?: 0f) >= goal
    }
    val goalsSet = categories.count { (it.goal ?: 0f) > 0f }
    val listed = remember(inPeriod, selectedCategory) {
        if (selectedCategory == null) inPeriod else inPeriod.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = { AceTopBar(title = "Goal progress", onBackClick = onBackClick) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            if (isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AceRed) }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "Time tracked",
                        value = TimeUtils.formatDurationShort(totalMillis),
                        icon = Lucide.Clock,
                        accent = AceRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Goals met",
                        value = if (goalsSet > 0) "$goalsMet / $goalsSet" else "—",
                        icon = Lucide.Target,
                        accent = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (categories.isEmpty() && !isLoading) {
                item {
                    EmptyState(
                        icon = Lucide.TrendingUp,
                        title = "No goals to track yet",
                        message = "Create a category with a target number of hours, then log time against it."
                    )
                }
            } else if (categories.isNotEmpty()) {
                item { SectionLabel("By category · tap to filter", modifier = Modifier.padding(top = 4.dp)) }
                item {
                    AceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                        categories
                            .sortedByDescending { hoursByCategory[it.name] ?: 0f }
                            .forEach { category ->
                                val isSelected = selectedCategory == category.name
                                Surface(
                                    onClick = { selectedCategory = if (isSelected) null else category.name },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                ) {
                                    CategoryProgressRow(
                                        category = category,
                                        hours = hoursByCategory[category.name] ?: 0f,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                    }
                }
            }

            item {
                SectionLabel(
                    text = (selectedCategory ?: "All sessions") + " · ${listed.size}",
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (listed.isEmpty() && !isLoading) {
                item {
                    EmptyState(
                        icon = Lucide.CalendarDays,
                        title = "No sessions in this period",
                        message = "Try a longer period or log some time."
                    )
                }
            }

            items(listed, key = { it.id.ifEmpty { "${it.startDate}${it.startTime}${it.category}" } }) { entry ->
                TimesheetEntryCard(entry = entry, color = categories.find { it.name == entry.category }?.colorHex)
            }
        }
    }
}

@Composable
fun TimesheetEntryCard(entry: TimesheetEntry, color: String? = null) {
    AceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color?.let { parseHexColor(it) } ?: AceRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.category,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = TimeUtils.formatDurationShort(entry.timeSpent),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (entry.endDate.isNotBlank() && entry.endDate != entry.startDate) {
                "${entry.startDate} ${entry.startTime} → ${entry.endDate} ${entry.endTime}"
            } else {
                "${entry.startDate} · ${entry.startTime} – ${entry.endTime}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (entry.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = entry.description, style = MaterialTheme.typography.bodyMedium)
        }

        if (entry.imageUrl.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = "Photo attached to ${entry.category} session",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
