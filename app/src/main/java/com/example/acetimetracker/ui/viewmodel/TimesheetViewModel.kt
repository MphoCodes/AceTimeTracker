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
    private val authRepository: AuthRepository = AuthRepository(),
    private val imageUploader: (Uri, (Resource<String>) -> Unit) -> Unit = timesheetRepository::uploadImage
) : ViewModel() {

    private val _categoriesState = MutableLiveData<Resource<List<Category>>>()
    val categoriesState: LiveData<Resource<List<Category>>> = _categoriesState

    private val _timesheetsState = MutableLiveData<Resource<List<TimesheetEntry>>>()
    val timesheetsState: LiveData<Resource<List<TimesheetEntry>>> = _timesheetsState

    private val _uploadImageState = MutableLiveData<Resource<String>?>()
    val uploadImageState: LiveData<Resource<String>?> = _uploadImageState

    private val _saveEntryState = MutableLiveData<Resource<Unit>?>()
    val saveEntryState: LiveData<Resource<Unit>?> = _saveEntryState

    private val _deleteEntryState = MutableLiveData<Resource<Unit>?>()
    val deleteEntryState: LiveData<Resource<Unit>?> = _deleteEntryState

    private var deletingEntryId: String? = null

    fun consumeDeleteState() {
        _deleteEntryState.value = null
    }

    var selectedImageUrl: String = ""
    private var imageUploadGeneration = 0L

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
        _timesheetsState.value = Resource.Loading
        timesheetRepository.fetchTimesheets(userId) { result ->
            _timesheetsState.value = result
        }
    }

    fun deleteTimesheet(entryId: String) {
        val userId = authRepository.currentUserId
        if (deletingEntryId != null) return
        if (userId.isEmpty() || entryId.isEmpty()) {
            _deleteEntryState.value = Resource.Error("Unable to delete this entry. Sign in and refresh your history.")
            return
        }
        deletingEntryId = entryId
        _deleteEntryState.value = Resource.Loading
        timesheetRepository.deleteTimesheetEntry(userId, entryId) { result ->
            if (result !is Resource.Loading) deletingEntryId = null
            _deleteEntryState.value = result
            if (result is Resource.Success) loadTimesheets()
        }
    }

    fun uploadImage(imageUri: Uri) {
        val generation = ++imageUploadGeneration
        selectedImageUrl = ""
        if (authRepository.isMockSession) {
            selectedImageUrl = imageUri.toString()
            _uploadImageState.value = Resource.Success(selectedImageUrl)
            return
        }
        imageUploader(imageUri) { result ->
            if (generation == imageUploadGeneration) {
                if (result is Resource.Success) {
                    selectedImageUrl = result.data
                }
                _uploadImageState.value = result
            }
        }
    }

    fun clearSelectedImage() {
        imageUploadGeneration++
        selectedImageUrl = ""
        _uploadImageState.value = null
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

        if (_saveEntryState.value is Resource.Loading) return

        if (_uploadImageState.value is Resource.Loading) {
            _saveEntryState.value = Resource.Error("Please wait for the photo to finish uploading")
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

        val timeSpentMillis = TimeUtils.calculateTimeSpentMillis(startDate, startTime, endDate, endTime)
        if (timeSpentMillis <= 0L) {
            _saveEntryState.value = Resource.Error("End time must be after the start time")
            return
        }

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

    /** Clears one-shot save/upload results after the UI has reacted, so they are not replayed on recreation. */
    fun consumeSaveState() {
        _saveEntryState.value = null
    }

    fun consumeUploadState() {
        _uploadImageState.value = null
    }
}
