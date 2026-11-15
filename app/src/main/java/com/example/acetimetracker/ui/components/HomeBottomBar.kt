package com.example.acetimetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ChartLineUp
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.House
import com.adamglin.phosphoricons.regular.Timer
import com.example.acetimetracker.ui.screen.Destination
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme

/** Shared primary destinations; state is owned by the main host. */
@Composable
fun HomeBottomBar(
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    selected: Destination = Destination.Today
) {
    val useShortLabels = LocalDensity.current.fontScale >= 1.3f
    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            shadowElevation = 8.dp
        ) {
            NavigationBar(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NavigationBarItem(
                    selected = selected == Destination.Today,
                    onClick = { onNavigate(Destination.Today) },
                    icon = { Icon(PhosphorIcons.Regular.House, null, Modifier.size(24.dp)) },
                    label = { Text("Today", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                    colors = colors
                )
                NavigationBarItem(
                    selected = selected == Destination.Timer,
                    onClick = { onNavigate(Destination.Timer) },
                    icon = { Icon(PhosphorIcons.Regular.Timer, null, Modifier.size(24.dp)) },
                    label = { Text("Timer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium) },
                    colors = colors
                )
                NavigationBarItem(
                    selected = selected == Destination.Timesheet,
                    onClick = { onNavigate(Destination.Timesheet) },
                    icon = { Icon(PhosphorIcons.Regular.FileText, null, Modifier.size(24.dp)) },
                    label = { Text(if (useShortLabels) "Log" else "Timesheet", modifier = Modifier.semantics { contentDescription = "Timesheet" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium) },
                    colors = colors
                )
                NavigationBarItem(
                    selected = selected == Destination.MyProgress,
                    onClick = { onNavigate(Destination.MyProgress) },
                    icon = { Icon(PhosphorIcons.Regular.ChartLineUp, null, Modifier.size(24.dp)) },
                    label = { Text(if (useShortLabels) "Stats" else "Progress", modifier = Modifier.semantics { contentDescription = "Progress" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium) },
                    colors = colors
                )
            }
        }
    }
}

@Preview(name = "Navigation · Light", widthDp = 393)
@Composable
private fun HomeBottomBarLightPreview() {
    AceTimeTrackerTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) { HomeBottomBar(onNavigate = {}) }
    }
}

@Preview(name = "Navigation · Dark", widthDp = 393)
@Composable
private fun HomeBottomBarDarkPreview() {
    AceTimeTrackerTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) { HomeBottomBar(onNavigate = {}) }
    }
}

@Preview(name = "Navigation · Large text", widthDp = 320, fontScale = 1.5f)
@Composable
private fun HomeBottomBarLargeTextPreview() {
    AceTimeTrackerTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) { HomeBottomBar(onNavigate = {}) }
    }
}
