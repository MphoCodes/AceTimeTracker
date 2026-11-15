package com.example.acetimetracker.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acetimetracker.AceTimeTrackerApp
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.CategoryRepository
import com.example.acetimetracker.data.repository.FavoriteCategories
import com.example.acetimetracker.util.Resource

class CategoryViewModel(
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val favorites by lazy { FavoriteCategories(AceTimeTrackerApp.context) }
    private val _favoriteIds = MutableLiveData<Set<String>>(emptySet())
    val favoriteIds: LiveData<Set<String>> = _favoriteIds
    private val _categoriesState = MutableLiveData<Resource<List<Category>>>()
    val categoriesState: LiveData<Resource<List<Category>>> = _categoriesState
    private val _addCategoryState = MutableLiveData<Resource<String>?>()
    val addCategoryState: LiveData<Resource<String>?> = _addCategoryState
    private val _updateCategoryState = MutableLiveData<Resource<Unit>?>()
    val updateCategoryState: LiveData<Resource<Unit>?> = _updateCategoryState
    private val _deleteCategoryState = MutableLiveData<Resource<Unit>>()
    val deleteCategoryState: LiveData<Resource<Unit>> = _deleteCategoryState

    fun resetSaveState() {
        if (_addCategoryState.value !is Resource.Loading) _addCategoryState.value = null
        if (_updateCategoryState.value !is Resource.Loading) _updateCategoryState.value = null
    }

    fun loadCategories() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _categoriesState.value = Resource.Error("User not logged in")
            _favoriteIds.value = emptySet()
            return
        }
        _favoriteIds.value = favorites.getIds(userId)
        categoryRepository.fetchCategories(userId) { result ->
            if (authRepository.currentUserId == userId) {
                _categoriesState.value = when (result) {
                    is Resource.Success -> Resource.Success(favorites.sort(userId, result.data))
                    else -> result
                }
            }
        }
    }

    fun toggleFavorite(category: Category) {
        val userId = authRepository.currentUserId
        _favoriteIds.value = favorites.toggle(userId, category.documentId)
        val categories = (_categoriesState.value as? Resource.Success)?.data ?: return
        _categoriesState.value = Resource.Success(favorites.sort(userId, categories))
    }

    fun addCategory(
        name: String, description: String, goal: Float?, minGoal: Float?, maxGoal: Float?,
        colorHex: String = "#DC2626"
    ) {
        if (_addCategoryState.value is Resource.Loading) return
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _addCategoryState.value = Resource.Error("User not logged in")
            return
        }
        val category = Category(name = name.trim(), description = description.trim(),
            goal = goal, minGoal = minGoal, maxGoal = maxGoal, colorHex = colorHex)
        val error = CategoryValidation.error(category, (_categoriesState.value as? Resource.Success)?.data.orEmpty())
        if (error != null) {
            _addCategoryState.value = Resource.Error(error)
            return
        }
        _addCategoryState.value = Resource.Loading
        categoryRepository.addCategory(userId, category) { result ->
            if (authRepository.currentUserId == userId) {
                _addCategoryState.value = result
                if (result is Resource.Success) loadCategories()
            }
        }
    }

    fun updateCategory(category: Category) {
        if (_updateCategoryState.value is Resource.Loading) return
        val userId = authRepository.currentUserId
        if (userId.isEmpty() || category.documentId.isBlank()) {
            _updateCategoryState.value = Resource.Error("Sign in and select a saved category to edit")
            return
        }
        val updated = category.copy(name = category.name.trim(), description = category.description.trim())
        val error = CategoryValidation.error(updated, (_categoriesState.value as? Resource.Success)?.data.orEmpty())
        if (error != null) {
            _updateCategoryState.value = Resource.Error(error)
            return
        }
        _updateCategoryState.value = Resource.Loading
        categoryRepository.updateCategory(userId, updated) { result ->
            if (authRepository.currentUserId == userId) {
                _updateCategoryState.value = result
                if (result is Resource.Success) loadCategories()
            }
        }
    }

    fun deleteCategory(category: Category) {
        if (_deleteCategoryState.value is Resource.Loading) return
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _deleteCategoryState.value = Resource.Error("User not logged in")
            return
        }
        _deleteCategoryState.value = Resource.Loading
        categoryRepository.deleteCategory(userId, category.documentId) { result ->
            if (authRepository.currentUserId == userId) {
                _deleteCategoryState.value = result
                if (result is Resource.Success) loadCategories()
            }
        }
    }
}
