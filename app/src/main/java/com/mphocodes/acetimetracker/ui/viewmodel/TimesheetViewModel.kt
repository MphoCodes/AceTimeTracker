package com.mphocodes.acetimetracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mphocodes.acetimetracker.data.model.Category
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import com.mphocodes.acetimetracker.data.repository.AuthRepository
import com.mphocodes.acetimetracker.data.repository.CategoryRepository
import com.mphocodes.acetimetracker.data.repository.TimesheetRepository
import com.mphocodes.acetimetracker.util.Resource
import com.mphocodes.acetimetracker.util.TimeUtils
import com.mphocodes.acetimetracker.util.EntryEditor
import com.mphocodes.acetimetracker.data.repository.FavoriteCategories
import com.mphocodes.acetimetracker.AceTimeTrackerApp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

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

    var editingEntry by mutableStateOf<TimesheetEntry?>(null)
        private set

    fun beginEdit(entry: TimesheetEntry) {
        if (_saveEntryState.value is Resource.Loading || entry.userId != authRepository.currentUserId) return
        clearSelectedImage()
        editingEntry = entry
        selectedImageUrl = entry.imageUrl
    }

    fun cancelEdit() {
        if (_saveEntryState.value is Resource.Loading) return
        editingEntry = null
        clearSelectedImage()
    }

    var selectedImageUrl by mutableStateOf("")
    private var imageUploadGeneration = 0L

    fun loadCategories() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _categoriesState.value = Resource.Error("User not authenticated")
            return
        }
        categoryRepository.fetchCategories(userId) { result ->
            if (result !is Resource.Loading || _categoriesState.value !is Resource.Success) {
                _categoriesState.value = if (result is Resource.Success) Resource.Success(FavoriteCategories(AceTimeTrackerApp.context).sort(userId, result.data)) else result
            }
        }
    }

    fun loadTimesheets() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _timesheetsState.value = Resource.Error("User not authenticated")
            return
        }
        timesheetRepository.fetchTimesheets(userId) { result ->
            if (result !is Resource.Loading || _timesheetsState.value !is Resource.Success) _timesheetsState.value = result
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

        val entry = try {
            EntryEditor.build(editingEntry, userId, category, startDate, startTime, endDate, endTime, description, selectedImageUrl)
        } catch (error: IllegalArgumentException) {
            _saveEntryState.value = Resource.Error(error.message ?: "Invalid entry")
            return
        }

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
