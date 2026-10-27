package com.example.acetimetracker.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.ui.screen.CategoriesScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.CategoryViewModel
import com.example.acetimetracker.util.Resource

class CategoriesActivity : ComponentActivity() {

    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModel()

        setContent {
            AceTimeTrackerTheme {
                val categoriesState by viewModel.categoriesState.observeAsState()
                val addCategoryState by viewModel.addCategoryState.observeAsState()

                CategoriesScreen(
                    categoriesState = categoriesState,
                    addCategoryState = addCategoryState,
                    onBackClick = { finish() },
                    onAddCategory = { name, desc, goal, minGoal, maxGoal ->
                        viewModel.addCategory(name, desc, goal, minGoal, maxGoal)
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
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.deleteCategoryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
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
