package com.example.acetimetracker.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.CategoryRepository
import com.example.acetimetracker.data.repository.TimesheetRepository
import com.example.acetimetracker.util.Resource

class ProgressViewModel(
    private val timesheetRepository: TimesheetRepository = TimesheetRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _categoriesState = MutableLiveData<Resource<List<Category>>>()
    val categoriesState: LiveData<Resource<List<Category>>> = _categoriesState

    private val _timesheetsState = MutableLiveData<Resource<List<TimesheetEntry>>>()
    val timesheetsState: LiveData<Resource<List<TimesheetEntry>>> = _timesheetsState

    private val _chartDataState = MutableLiveData<Resource<Map<String, Float>>>()
    val chartDataState: LiveData<Resource<Map<String, Float>>> = _chartDataState

    fun loadCategories() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _categoriesState.value = Resource.Error("User not authenticated")
            return
        }
        categoryRepository.fetchCategories(userId) { result ->
            _categoriesState.value = result
        }
    }

    fun loadTimesheets() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _timesheetsState.value = Resource.Error("User not authenticated")
            return
        }
        timesheetRepository.fetchTimesheets(userId) { result ->
            _timesheetsState.value = result
        }
    }

    fun filterByCategory(category: String) {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _timesheetsState.value = Resource.Error("User not authenticated")
            return
        }
        timesheetRepository.fetchTimesheetsByCategory(userId, category) { result ->
            _timesheetsState.value = result
        }
    }

    fun loadChartData() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _chartDataState.value = Resource.Error("User not authenticated")
            return
        }
        timesheetRepository.fetchChartData(userId) { result ->
            _chartDataState.value = result
        }
    }
}
