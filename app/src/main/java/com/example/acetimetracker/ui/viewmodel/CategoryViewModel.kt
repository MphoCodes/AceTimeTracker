package com.example.acetimetracker.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.CategoryRepository
import com.example.acetimetracker.util.Resource

class CategoryViewModel(
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _categoriesState = MutableLiveData<Resource<List<Category>>>()
    val categoriesState: LiveData<Resource<List<Category>>> = _categoriesState

    private val _addCategoryState = MutableLiveData<Resource<String>>()
    val addCategoryState: LiveData<Resource<String>> = _addCategoryState

    private val _deleteCategoryState = MutableLiveData<Resource<Unit>>()
    val deleteCategoryState: LiveData<Resource<Unit>> = _deleteCategoryState

    fun loadCategories() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _categoriesState.value = Resource.Error("User not logged in")
            return
        }
        categoryRepository.fetchCategories(userId) { result ->
            _categoriesState.value = result
        }
    }

    fun addCategory(
        name: String,
        description: String,
        goal: Float?,
        minGoal: Float?,
        maxGoal: Float?,
        colorHex: String = "#DC2626"
    ) {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _addCategoryState.value = Resource.Error("User not logged in")
            return
        }

        if (name.isBlank()) {
            _addCategoryState.value = Resource.Error("Please enter a category name")
            return
        }

        if (listOfNotNull(goal, minGoal, maxGoal).any { it < 0f }) {
            _addCategoryState.value = Resource.Error("Goals can't be negative")
            return
        }

        if (minGoal != null && maxGoal != null && minGoal > maxGoal) {
            _addCategoryState.value = Resource.Error("Minimum goal can't be more than the maximum")
            return
        }

        val existing = (_categoriesState.value as? Resource.Success)?.data.orEmpty()
        if (existing.any { it.name.equals(name.trim(), ignoreCase = true) }) {
            _addCategoryState.value = Resource.Error("A category named '${name.trim()}' already exists")
            return
        }

        val category = Category(
            name = name.trim(),
            description = description.trim(),
            goal = goal,
            minGoal = minGoal,
            maxGoal = maxGoal,
            colorHex = colorHex
        )

        categoryRepository.addCategory(userId, category) { result ->
            _addCategoryState.value = result
            if (result is Resource.Success) {
                loadCategories()
            }
        }
    }

    fun deleteCategory(category: Category) {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _deleteCategoryState.value = Resource.Error("User not logged in")
            return
        }

        categoryRepository.deleteCategory(userId, category.documentId) { result ->
            _deleteCategoryState.value = result
            if (result is Resource.Success) {
                loadCategories()
            }
        }
    }
}
