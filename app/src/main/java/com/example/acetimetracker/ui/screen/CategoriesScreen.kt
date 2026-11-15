package com.example.acetimetracker.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.acetimetracker.ui.viewmodel.CategoryValidation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.acetimetracker.data.model.Category
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import com.example.acetimetracker.ui.components.AceAlertDialog
import com.example.acetimetracker.ui.theme.*
import com.example.acetimetracker.util.Resource

val CATEGORY_PALETTES = listOf(
    "#DC2626" to "Ruby",
    "#2563EB" to "Cobalt",
    "#10B981" to "Emerald",
    "#F59E0B" to "Amber",
    "#8B5CF6" to "Purple",
    "#EC4899" to "Pink"
)

fun parseHexColor(hex: String, fallback: Color = AceRed): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categoriesState: Resource<List<Category>>?,
    addCategoryState: Resource<*>?,
    onBackClick: () -> Unit,
    onAddCategory: (String, String, Float?, Float?, Float?, String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    updateCategoryState: Resource<Unit>? = null,
    favoriteIds: Set<String> = emptySet(),
    onUpdateCategory: (Category) -> Unit = {},
    onToggleFavorite: (Category) -> Unit = {},
    onResetSaveState: () -> Unit = {}
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingCategory by rememberSaveable { mutableStateOf<Category?>(null) }
    val saveState = if (editingCategory != null) updateCategoryState else addCategoryState
    LaunchedEffect(saveState) {
        if (saveState is Resource.Success && showAddDialog) {
            showAddDialog = false
            editingCategory = null
        }
    }
    var selectedCategoryForDetails by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Task Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            if ((categoriesState as? Resource.Success)?.data?.isNotEmpty() == true) {
                ExtendedFloatingActionButton(
                    onClick = { onResetSaveState(); editingCategory = null; showAddDialog = true },
                    containerColor = AceRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(PhosphorIcons.Regular.Plus, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("New category", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (categoriesState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AceRed
                    )
                }
                is Resource.Success -> {
                    val categories = categoriesState.data
                    if (categories.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No categories yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Categories group your sessions — reading, exercise, a side project — and hold the hour goals you're aiming for.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { onResetSaveState(); editingCategory = null; showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AceRed, contentColor = Color.White),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(PhosphorIcons.Regular.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create your first category", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(categories, key = { it.documentId.ifEmpty { it.name } }) { category ->
                                CategoryCard(
                                    category = category,
                                    onClick = { selectedCategoryForDetails = category },
                                    onDeleteClick = { categoryToDelete = category },
                                    isFavorite = category.documentId in favoriteIds,
                                    onFavoriteClick = { onToggleFavorite(category) }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${categoriesState.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                null -> {}
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        AddCategoryDialog(
            isLoading = saveState is Resource.Loading,
            initialCategory = editingCategory,
            errorMessage = (saveState as? Resource.Error)?.message,
            onDismiss = { if (saveState !is Resource.Loading) { showAddDialog = false; editingCategory = null } },
            onConfirm = { name, desc, goal, minGoal, maxGoal, colorHex ->
                val original = editingCategory
                if (original == null) onAddCategory(name, desc, goal, minGoal, maxGoal, colorHex)
                else onUpdateCategory(original.copy(name = name, description = desc, goal = goal,
                    minGoal = minGoal, maxGoal = maxGoal, colorHex = colorHex))
            }
        )
    }

    // Details Dialog
    if (selectedCategoryForDetails != null) {
        val cat = selectedCategoryForDetails!!
        AceAlertDialog(
            onDismissRequest = { selectedCategoryForDetails = null },
            icon = { Icon(PhosphorIcons.Regular.Folder, contentDescription = null, tint = parseHexColor(cat.colorHex), modifier = Modifier.size(24.dp)) },
            title = { Text(cat.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (cat.description.isNotEmpty()) {
                        Text(
                            text = cat.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    cat.goal?.let {
                        Text("Target: ${formatHours(it)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    cat.minGoal?.let {
                        Text("Minimum: ${formatHours(it)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    cat.maxGoal?.let {
                        Text("Maximum: ${formatHours(it)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (cat.goal == null && cat.minGoal == null && cat.maxGoal == null && cat.description.isEmpty()) {
                        Text("No goals set for this category.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onResetSaveState()
                    editingCategory = cat
                    selectedCategoryForDetails = null
                    showAddDialog = true
                }) { Text("Edit category") }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForDetails = null }) { Text("Close") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AceAlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(PhosphorIcons.Regular.Trash, contentDescription = null, tint = AceRed, modifier = Modifier.size(24.dp)) },
            title = { Text("Delete Category?") },
            text = { Text("'${cat.name}' will be removed. Sessions you already logged keep their history.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AceRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    val catColor = parseHexColor(category.colorHex)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Folder,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(24.dp)
                    )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (category.description.isNotEmpty()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    category.goal?.let { goal ->
                        GoalChip(label = "Target ${formatHours(goal)}", tint = AccentEmerald)
                    }
                    category.minGoal?.let { min ->
                        GoalChip(label = "Min ${formatHours(min)}", tint = AccentBlue)
                    }
                    category.maxGoal?.let { max ->
                        GoalChip(label = "Max ${formatHours(max)}", tint = AccentAmber)
                    }
                }
            }

            Column {
                IconToggleButton(checked = isFavorite, onCheckedChange = { onFavoriteClick() }) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Star,
                        contentDescription = if (isFavorite) "Remove ${category.name} from favorites" else "Favorite ${category.name}",
                        tint = if (isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Trash,
                        contentDescription = "Delete ${category.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalChip(label: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

/** Formats an hour goal without trailing ".0", e.g. 2f -> "2h", 1.5f -> "1.5h". */
fun formatHours(hours: Float): String {
    val rounded = (hours * 10).toInt() / 10f
    return if (rounded % 1f == 0f) "${rounded.toInt()}h" else "${rounded}h"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, goal: Float?, minGoal: Float?, maxGoal: Float?, colorHex: String) -> Unit,
    initialCategory: Category? = null,
    errorMessage: String? = null
) {
    var name by rememberSaveable(initialCategory?.documentId) { mutableStateOf(initialCategory?.name.orEmpty()) }
    var desc by rememberSaveable(initialCategory?.documentId) { mutableStateOf(initialCategory?.description.orEmpty()) }
    var goalText by rememberSaveable(initialCategory?.documentId) { mutableStateOf(initialCategory?.goal?.toString().orEmpty()) }
    var minGoalText by rememberSaveable(initialCategory?.documentId) { mutableStateOf(initialCategory?.minGoal?.toString().orEmpty()) }
    var maxGoalText by rememberSaveable(initialCategory?.documentId) { mutableStateOf(initialCategory?.maxGoal?.toString().orEmpty()) }
    var selectedColorHex by rememberSaveable(initialCategory?.documentId) { mutableStateOf(initialCategory?.colorHex ?: CATEGORY_PALETTES.first().first) }
    var inputError by remember { mutableStateOf<String?>(null) }

    AceAlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(if (initialCategory == null) "New category" else "Edit category", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                (inputError ?: errorMessage)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (initialCategory != null) {
                    Text("Renaming keeps the original category label on past sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    placeholder = { Text("e.g. Reading") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Colour",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CATEGORY_PALETTES.forEach { (hex, name) ->
                        val color = parseHexColor(hex)
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color = color, shape = CircleShape)
                                .clickable(onClickLabel = "Choose $name") { selectedColorHex = hex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("Target hours (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = minGoalText,
                        onValueChange = { minGoalText = it },
                        label = { Text("Min hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = maxGoalText,
                        onValueChange = { maxGoalText = it },
                        label = { Text("Max hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    inputError = CategoryValidation.goalInputError(goalText, minGoalText, maxGoalText)
                    if (inputError != null) return@Button
                    val goal = goalText.toFloatOrNull()
                    val min = minGoalText.toFloatOrNull()
                    val max = maxGoalText.toFloatOrNull()
                    onConfirm(name.trim(), desc.trim(), goal, min, max, selectedColorHex)
                },
                enabled = name.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AceRed, contentColor = Color.White)
            ) {
                Text(if (isLoading) "Saving…" else if (initialCategory == null) "Create" else "Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
