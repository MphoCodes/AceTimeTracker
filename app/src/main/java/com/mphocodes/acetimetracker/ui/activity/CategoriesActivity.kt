package com.mphocodes.acetimetracker.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.mphocodes.acetimetracker.ui.screen.CategoriesScreen
import com.mphocodes.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.mphocodes.acetimetracker.ui.viewmodel.CategoryViewModel
import com.mphocodes.acetimetracker.util.Resource

class CategoriesActivity : ComponentActivity() {

    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModel()

        setContent {
            AceTimeTrackerTheme {
                val categoriesState by viewModel.categoriesState.observeAsState()
                val addCategoryState by viewModel.addCategoryState.observeAsState()
                val updateCategoryState by viewModel.updateCategoryState.observeAsState()
                val favoriteIds by viewModel.favoriteIds.observeAsState(emptySet())

                CategoriesScreen(
                    categoriesState = categoriesState,
                    addCategoryState = addCategoryState,
                    updateCategoryState = updateCategoryState,
                    favoriteIds = favoriteIds,
                    onUpdateCategory = viewModel::updateCategory,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onResetSaveState = viewModel::resetSaveState,
                    onBackClick = { finish() },
                    onAddCategory = { name, desc, goal, minGoal, maxGoal, colorHex ->
                        viewModel.addCategory(name, desc, goal, minGoal, maxGoal, colorHex)
                    },
                    onDeleteCategory = { category ->
                        viewModel.deleteCategory(category)
                    }
                )
            }
        }

        viewModel.loadCategories()
    }

    private fun observeViewModel() {
        viewModel.addCategoryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading, null -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.updateCategoryState.observe(this) { resource ->
            if (resource is Resource.Success) {
                Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteCategoryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading, null -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
