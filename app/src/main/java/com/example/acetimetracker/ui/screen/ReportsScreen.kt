package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CalendarDots
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.ChartLineUp
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.report.CategoryReport
import com.example.acetimetracker.report.DailyTimeline
import com.example.acetimetracker.report.ReportCalculator
import com.example.acetimetracker.report.ReportFiles
import com.example.acetimetracker.report.ReportFormat
import com.example.acetimetracker.report.ReportRange
import com.example.acetimetracker.report.ReportSummary
import com.example.acetimetracker.report.TimelineSession
import com.example.acetimetracker.ui.components.AceCard
import com.example.acetimetracker.ui.components.AceDatePickerDialog
import com.example.acetimetracker.ui.components.AceTopBar
import com.example.acetimetracker.ui.components.EmptyState
import com.example.acetimetracker.ui.components.SectionLabel
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(
    entries: List<TimesheetEntry>,
    categories: List<Category>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onStartSession: () -> Unit = {},
) {
    val today = LocalDate.now()
    val initial = ReportRange.week(today)
    var from by rememberSaveable { mutableStateOf(initial.start.toString()) }
    var until by rememberSaveable { mutableStateOf(initial.endInclusive.toString()) }
    var dayText by rememberSaveable { mutableStateOf(today.toString()) }
    var showRange by rememberSaveable { mutableStateOf(false) }
    var showDay by rememberSaveable { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val range = ReportRange.parse(from, until) ?: initial
    val day = LocalDate.parse(dayText).coerceIn(range.start, range.endInclusive)
    val report by produceState<ReportSummary?>(null, entries, categories, range) {
        value = null
        value = withContext(Dispatchers.Default) { ReportCalculator.summarize(entries, categories, range) }
    }
    val timeline by produceState<DailyTimeline?>(null, entries, day) {
        value = null
        value = withContext(Dispatchers.Default) { ReportCalculator.timeline(entries, day) }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun selectRange(selected: ReportRange) {
        from = selected.start.toString()
        until = selected.endInclusive.toString()
        dayText = today.coerceIn(selected.start, selected.endInclusive).toString()
    }
    ReportsContent(
        report = report?.takeIf { it.range == range },
        timeline = timeline?.takeIf { it.day == day },
        range = range,
        day = day,
        exporting = exporting,
        snackbar = snackbar,
        onBackClick = onBackClick,
        onStartSession = onStartSession,
        onWeek = { selectRange(ReportRange.week(today)) },
        onMonth = { selectRange(ReportRange.month(today)) },
        onCustom = { showRange = true },
        onPreviousDay = { dayText = day.minusDays(1).toString() },
        onNextDay = { dayText = day.plusDays(1).toString() },
        onPickDay = { showDay = true },
        onExport = { format ->
            val snapshot = report?.takeIf { it.range == range }
            if (snapshot != null && !exporting) {
                exporting = true
                scope.launch {
                    try {
                        val file = ReportFiles.create(context.applicationContext, snapshot, format)
                        ReportFiles.share(context, file, format)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        snackbar.showSnackbar("Could not share the report. Please try again.")
                    } finally {
                        exporting = false
                    }
                }
            }
        },
        modifier = modifier,
    )
    if (showRange) {
        CustomReportRangeDialog(range = range, onDismiss = { showRange = false }, onApply = {
            selectRange(it)
            showRange = false
        })
    }
    if (showDay) {
        AceDatePickerDialog(initialDate = day.toString(), onDismiss = { showDay = false }, onConfirm = {
            val selected = LocalDate.parse(it)
            dayText = selected.toString()
            if (selected < range.start || selected > range.endInclusive) selectRange(ReportRange(selected, selected))
            showDay = false
        })
    }
}

@Composable
private fun ReportsContent(
    report: ReportSummary?,
    timeline: DailyTimeline?,
    range: ReportRange,
    day: LocalDate,
    exporting: Boolean,
    snackbar: SnackbarHostState,
    onBackClick: () -> Unit,
    onWeek: () -> Unit,
    onMonth: () -> Unit,
    onCustom: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDay: () -> Unit,
    onExport: (ReportFormat) -> Unit,
    modifier: Modifier = Modifier,
    onStartSession: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { AceTopBar(title = "Reports", subtitle = "See where your time goes", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = range == ReportRange.week(LocalDate.now()), onClick = onWeek, label = { Text("This week") })
                    FilterChip(selected = range == ReportRange.month(LocalDate.now()), onClick = onMonth, label = { Text("This month") })
                    FilterChip(selected = range != ReportRange.week(LocalDate.now()) && range != ReportRange.month(LocalDate.now()),
                        onClick = onCustom, label = { Text("Custom range") })
                }
                Text(text = "${range.start}  →  ${range.endInclusive}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (report == null) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            } else {
                if (report.sessions.isEmpty()) {
                    item {
                        AceCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Your time tells a story", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Text("No sessions in this period yet. Start tracking, or choose a different date range.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth()) { Text("Start session") }
                            TextButton(onClick = onCustom, modifier = Modifier.fillMaxWidth()) { Text("Choose another period") }
                        }
                    }
                    if (report.invalidEntryCount > 0) item {
                        Text("${report.invalidEntryCount} invalid record(s) omitted.", color = MaterialTheme.colorScheme.error)
                    }
                    return@LazyColumn
                }
                item { ReportOverview(report = report, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onExport(ReportFormat.PDF) }, enabled = !exporting, modifier = Modifier.weight(1f)) { Text("Share PDF") }
                        OutlinedButton(onClick = { onExport(ReportFormat.CSV) }, enabled = !exporting, modifier = Modifier.weight(1f)) { Text("Share CSV") }
                    }
                    if (exporting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                item { SectionLabel(text = "Category goals & actuals") }
                item {
                    Text(text = "Configured targets compared with time in this range.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(report.categories) { category -> CategoryGoalCard(category = category, modifier = Modifier.fillMaxWidth()) }
                if (report.sessions.isEmpty()) {
                    item { EmptyState(icon = PhosphorIcons.Regular.ChartLineUp, title = "No time in this range", message = "Choose another period or save a session to see your progress.") }
                }
                if (report.invalidEntryCount > 0) {
                    item { Text(text = "${report.invalidEntryCount} record(s) with invalid dates or durations omitted.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }
            item { SectionLabel(text = "Daily timeline") }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousDay, enabled = day > range.start) {
                        Icon(imageVector = PhosphorIcons.Regular.CaretLeft, contentDescription = "Previous day")
                    }
                    TextButton(onClick = onPickDay, modifier = Modifier.weight(1f)) {
                        Text(text = day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")))
                    }
                    IconButton(onClick = onNextDay, enabled = day < range.endInclusive) {
                        Icon(imageVector = PhosphorIcons.Regular.CaretRight, contentDescription = "Next day")
                    }
                }
            }
            if (timeline == null) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            } else {
                if (timeline.overlapMillis > 0) {
                    item { Text(text = "${TimeUtils.formatDurationShort(timeline.overlapMillis)} of overlapping session spans",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
                if (timeline.sessions.isEmpty()) {
                    item { Text(text = "No sessions on this day.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                timeline.sessions.forEachIndexed { index, item ->
                    timeline.gaps.firstOrNull { it.end == item.session.start }?.let { gap ->
                        item(key = "gap-$index") {
                            Text(text = "${gap.start.toLocalTime()} – ${gap.end.toLocalTime()} · ${TimeUtils.formatDurationShort(gap.millis)} gap",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    item(key = "session-$index") { TimelineSessionCard(item = item, modifier = Modifier.fillMaxWidth()) }
                }
            }
            item {
                Text(text = "Timeline shows clock spans, including pauses. Overnight sessions are clipped to the selected day. Reports count recorded time; overlapping sessions both contribute.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReportOverview(report: ReportSummary, modifier: Modifier = Modifier) {
    AceCard(modifier = modifier) {
        Text(text = "TIME TRACKED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = TimeUtils.formatDurationShort(report.totalMillis), style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = "${report.sessions.size} ${if (report.sessions.size == 1) "session" else "sessions"} · ${report.range.days} ${if (report.range.days == 1L) "day" else "days"}", style = MaterialTheme.typography.bodyMedium)
        val prior = report.range.previous()
        val comparison = report.changePercent?.let { String.format(Locale.getDefault(), "%+.1f%% vs previous period", it) }
            ?: if (report.totalMillis > 0) "No tracked time in the previous period" else "No tracked time in either period"
        Text(text = comparison, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
        Text(text = "${prior.start} – ${prior.endInclusive} · ${TimeUtils.formatDurationShort(report.previousMillis)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryGoalCard(category: CategoryReport, modifier: Modifier = Modifier) {
    AceCard(modifier = modifier) {
        Text(text = category.name.ifBlank { "Uncategorized" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        val goal = category.goalHours?.let { String.format(Locale.getDefault(), "%.1f h target", it) } ?: "No target set"
        Text(text = "${TimeUtils.formatDurationShort(category.trackedMillis)} actual · $goal", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        category.goalFraction?.let { progress ->
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

@Composable
private fun TimelineSessionCard(item: TimelineSession, modifier: Modifier = Modifier) {
    AceCard(modifier = modifier) {
        val session = item.session
        Text(text = "${session.start.toLocalTime()} – ${if (session.end.toLocalDate() > session.start.toLocalDate()) "24:00" else session.end.toLocalTime()}",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = session.entry.category.ifBlank { "Uncategorized" }, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(text = "${TimeUtils.formatDurationShort(session.trackedMillis)} recorded", style = MaterialTheme.typography.bodySmall)
        if (session.entry.description.isNotBlank()) Text(text = session.entry.description, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp))
        if (item.overlapsPrevious) Text(text = "Overlaps an earlier session", color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CustomReportRangeDialog(range: ReportRange, onDismiss: () -> Unit, onApply: (ReportRange) -> Unit, modifier: Modifier = Modifier) {
    var from by rememberSaveable { mutableStateOf(range.start.toString()) }
    var until by rememberSaveable { mutableStateOf(range.endInclusive.toString()) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = ReportRange.parse(from, until)
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Custom date range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = from, onValueChange = { from = it }, label = { Text("From · YYYY-MM-DD") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                        IconButton(onClick = { picker = "from" }) { Icon(PhosphorIcons.Regular.CalendarDots, contentDescription = "Pick start date") }
                    })
                OutlinedTextField(value = until, onValueChange = { until = it }, label = { Text("Through · YYYY-MM-DD") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                        IconButton(onClick = { picker = "until" }) { Icon(PhosphorIcons.Regular.CalendarDots, contentDescription = "Pick end date") }
                    })
                if (selected == null) Text("Enter valid dates with the end on or after the start.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { selected?.let(onApply) }, enabled = selected != null) { Text("Apply range") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    picker?.let { field ->
        AceDatePickerDialog(initialDate = if (field == "from") from else until, onDismiss = { picker = null }, onConfirm = {
            if (field == "from") from = it else until = it
            picker = null
        })
    }
}

@Preview(showBackground = true, name = "Progress light")
@Preview(showBackground = true, name = "Progress dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReportsPreview() {
    val day = LocalDate.of(2026, 11, 15)
    val range = ReportRange.week(day)
    val entries = listOf(TimesheetEntry(category = "Development", startDate = day.toString(), startTime = "09:00",
        endDate = day.toString(), endTime = "11:30", timeSpent = 9_000_000))
    val categories = listOf(Category(name = "Development", goal = 12f), Category(name = "Learning", goal = 4f))
    AceTimeTrackerTheme {
        ReportsContent(report = ReportCalculator.summarize(entries, categories, range), timeline = ReportCalculator.timeline(entries, day),
            range = range, day = day, exporting = false, snackbar = remember { SnackbarHostState() }, onBackClick = {}, onWeek = {},
            onMonth = {}, onCustom = {}, onPreviousDay = {}, onNextDay = {}, onPickDay = {}, onExport = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportDetailsPreview() {
    AceTimeTrackerTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
            val range = ReportRange(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15))
            ReportOverview(report = ReportSummary(range, emptyList(), 7_200_000, 3_600_000, emptyList(), 0))
            CategoryGoalCard(category = CategoryReport("Learning", 3_600_000, 4.0))
            val entry = TimesheetEntry(category = "Development", description = "Build the daily report")
            TimelineSessionCard(item = TimelineSession(com.example.acetimetracker.report.ReportSession(entry,
                range.start.atTime(9, 0), range.start.atTime(10, 0), 3_600_000), true))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomRangePreview() {
    AceTimeTrackerTheme {
        CustomReportRangeDialog(range = ReportRange.week(LocalDate.of(2026, 11, 15)), onDismiss = {}, onApply = {})
    }
}
