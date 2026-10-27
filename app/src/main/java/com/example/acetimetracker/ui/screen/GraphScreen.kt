package com.example.acetimetracker.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.ui.components.AceCard
import com.example.acetimetracker.ui.components.AceTopBar
import com.example.acetimetracker.ui.components.EmptyState
import com.example.acetimetracker.ui.components.SectionLabel
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource
import java.util.Locale

/** Fallback colours for categories that no longer exist or have no stored colour. */
private val fallbackPalette = listOf(AceRed, AccentBlue, AccentEmerald, AccentAmber, AccentIndigo, Color(0xFFEC4899))

@Composable
fun GraphScreen(
    chartDataState: Resource<Map<String, Float>>?,
    onBackClick: () -> Unit,
    categoriesState: Resource<List<Category>>? = null
) {
    val categoryTimeMap = (chartDataState as? Resource.Success)?.data ?: emptyMap()
    val categories = (categoriesState as? Resource.Success)?.data ?: emptyList()
    val totalHours = categoryTimeMap.values.sum()

    val slices = remember(categoryTimeMap, categories) {
        categoryTimeMap.entries.mapIndexed { index, (name, hours) ->
            val color = categories.find { it.name == name }?.colorHex?.let { parseHexColor(it) }
                ?: fallbackPalette[index % fallbackPalette.size]
            Triple(name, hours, color)
        }
    }

    Scaffold(
        topBar = { AceTopBar(title = "Time breakdown", subtitle = "All time", onBackClick = onBackClick) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                chartDataState == null || chartDataState is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AceRed)
                }
                chartDataState is Resource.Error -> {
                    EmptyState(
                        icon = PhosphorIcons.Regular.ChartPie,
                        title = "Couldn't load your data",
                        message = chartDataState.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                categoryTimeMap.isEmpty() || totalHours <= 0f -> {
                    EmptyState(
                        icon = PhosphorIcons.Regular.ChartPie,
                        title = "No data yet",
                        message = "Log some time and you'll see how it splits across your categories.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    DonutChart(
                                        slices = slices.map { it.second to it.third },
                                        modifier = Modifier
                                            .size(220.dp)
                                            .semantics {
                                                contentDescription = slices.joinToString { (name, hours, _) ->
                                                    "$name ${percentOf(hours, totalHours)} percent"
                                                }
                                            }
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatHours(totalHours),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "total",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item { SectionLabel("Categories", modifier = Modifier.padding(top = 4.dp)) }

                        items(slices, key = { it.first }) { (category, hours, color) ->
                            val pct = percentOf(hours, totalHours)
                            AceCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatHours(hours),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.widthIn(min = 40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { hours / totalHours },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun percentOf(part: Float, total: Float): Int =
    if (total > 0f) String.format(Locale.US, "%.0f", part / total * 100).toInt() else 0

@Composable
private fun DonutChart(slices: List<Pair<Float, Color>>, modifier: Modifier = Modifier) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(slices) { sweep.animateTo(1f, tween(durationMillis = 700)) }
    val total = slices.sumOf { it.first.toDouble() }.toFloat()
    val gapColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val stroke = 28.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        var start = -90f
        slices.forEach { (value, color) ->
            val angle = if (total > 0f) value / total * 360f * sweep.value else 0f
            drawArc(color, start, angle, false, topLeft, arcSize, style = Stroke(stroke))
            if (slices.size > 1) {
                drawArc(gapColor, start + angle - 0.8f, 1.6f, false, topLeft, arcSize, style = Stroke(stroke))
            }
            start += angle
        }
    }
}
