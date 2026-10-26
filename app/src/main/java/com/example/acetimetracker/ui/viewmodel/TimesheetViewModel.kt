package com.example.acetimetracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.CategoryRepository
import com.example.acetimetracker.data.repository.TimesheetRepository
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils

class TimesheetViewModel(
    private val timesheetRepository: TimesheetRepository = TimesheetRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _categoriesState = MutableLiveData<Resource<List<Category>>>()
    val categoriesState: LiveData<Resource<List<Category>>> = _categoriesState

    private val _uploadImageState = MutableLiveData<Resource<String>>()
    val uploadImageState: LiveData<Resource<String>> = _uploadImageState

    private val _saveEntryState = MutableLiveData<Resource<Unit>>()
    val saveEntryState: LiveData<Resource<Unit>> = _saveEntryState

    var selectedImageUrl: String = ""

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

    fun uploadImage(imageUri: Uri) {
        timesheetRepository.uploadImage(imageUri) { result ->
            _uploadImageState.value = result
            if (result is Resource.Success) {
                selectedImageUrl = result.data
            }
        }
    }

    fun saveTimesheetEntry(
        category: String,
        startDate: String,
        startTime: String,
        endDate: String,
        endTime: String,
        description: String
    ) {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _saveEntryState.value = Resource.Error("User not authenticated")
            return
        }

        if (category.isBlank() || category == "Select Category") {
            _saveEntryState.value = Resource.Error("Please select a category")
            return
        }

        if (!TimeUtils.isValidDate(startDate) || !TimeUtils.isValidDate(endDate)) {
            _saveEntryState.value = Resource.Error("Please enter valid dates (YYYY-MM-DD)")
            return
        }

        if (!TimeUtils.isValidTime(startTime) || !TimeUtils.isValidTime(endTime)) {
            _saveEntryState.value = Resource.Error("Please enter valid times (HH:MM)")
            return
        }

        val timeSpentMillis = TimeUtils.calculateTimeSpentMillis(startTime, endTime)
        val entry = TimesheetEntry(
            userId = userId,
            category = category,
            startDate = startDate,
            startTime = startTime,
            endDate = endDate,
            endTime = endTime,
            timeSpent = timeSpentMillis,
            imageUrl = selectedImageUrl,
            description = description.trim()
        )

        timesheetRepository.saveTimesheetEntry(userId, entry) { result ->
            _saveEntryState.value = result
        }
    }
}
