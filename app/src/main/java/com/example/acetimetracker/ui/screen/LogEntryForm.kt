package com.example.acetimetracker.ui.screen

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.ui.theme.AceRed
import com.example.acetimetracker.ui.theme.AceRedDark
import com.example.acetimetracker.util.TimeUtils

/** The "Log entry" tab: a live duration hero, colour-coded category chips, tappable times and a save bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryForm(
    categories: List<Category>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    startDate: String,
    startTime: String,
    endDate: String,
    endTime: String,
    onStartDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    calculatedDuration: Long,
    hasCompleteRange: Boolean,
    selectedImageUri: Uri?,
    isUploading: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    onSelectImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        DurationHero(
            calculatedDuration = calculatedDuration,
            hasCompleteRange = hasCompleteRange,
            hasCategory = selectedCategory.isNotBlank()
        )

        FormSection("What did you work on?") {
            if (categories.isEmpty()) {
                Text(
                    "Create a category first, then come back to log time against it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val color = parseHexColor(cat.colorHex)
                        val isSelected = cat.name == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelect(cat.name) },
                            label = {
                                Text(
                                    cat.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = color,
                                selectedBorderWidth = 1.5.dp
                            ),
                            modifier = Modifier.height(42.dp)
                        )
                    }
                }
            }
        }

        FormSection("When") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerTile(
                    label = "Start date",
                    value = startDate,
                    icon = PhosphorIcons.Regular.CalendarBlank,
                    onClick = onStartDateClick,
                    modifier = Modifier.weight(1f)
                )
                PickerTile(
                    label = "Start time",
                    value = startTime,
                    icon = PhosphorIcons.Regular.Clock,
                    onClick = onStartTimeClick,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerTile(
                    label = "End date",
                    value = endDate,
                    icon = PhosphorIcons.Regular.CalendarBlank,
                    onClick = onEndDateClick,
                    modifier = Modifier.weight(1f)
                )
                PickerTile(
                    label = "End time",
                    value = endTime,
                    icon = PhosphorIcons.Regular.Clock,
                    onClick = onEndTimeClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        FormSection("Notes") {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("What did you get done?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                shape = RoundedCornerShape(18.dp),
                minLines = 3
            )
        }

        FormSection("Proof (optional)") {
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = { onImageClick(selectedImageUri.toString()) }),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .clickable(onClick = onRemoveImageClick)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                PhosphorIcons.Regular.X,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = onSelectImageClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            PhosphorIcons.Regular.Camera,
                            contentDescription = null,
                            tint = AceRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (isUploading) "Uploading photo…" else "Add a photo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Screenshots, pages, or a picture of your work",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = onSaveClick,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AceRed,
                contentColor = Color.White,
                disabledContainerColor = AceRed.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.85f)
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(PhosphorIcons.Regular.FloppyDisk, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when {
                        isUploading -> "Waiting for photo…"
                        canSave -> "Save ${TimeUtils.formatDurationShort(calculatedDuration)}"
                        else -> "Save entry"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DurationHero(
    calculatedDuration: Long,
    hasCompleteRange: Boolean,
    hasCategory: Boolean
) {
    val message = when {
        hasCompleteRange && calculatedDuration <= 0L -> "The end has to be after the start."
        !hasCompleteRange -> "Pick when you worked to see your total."
        !hasCategory -> "Next, choose what you worked on."
        else -> "Looking good. Ready when you are."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(AceRed, AceRedDark)))
            .padding(22.dp)
            .animateContentSize()
    ) {
        Icon(
            PhosphorIcons.Regular.Timer,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(64.dp)
        )
        Column {
            Text(
                "TIME TO LOG",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (calculatedDuration > 0L) TimeUtils.formatDurationShort(calculatedDuration) else "0m",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

@Composable
private fun PickerTile(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AceRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value.ifBlank { "Tap to set" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
