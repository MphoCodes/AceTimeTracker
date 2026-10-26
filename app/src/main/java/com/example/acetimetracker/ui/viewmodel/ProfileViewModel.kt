package com.example.acetimetracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.UserRepository
import com.example.acetimetracker.util.Resource

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _userProfileState = MutableLiveData<Resource<UserProfile>>()
    val userProfileState: LiveData<Resource<UserProfile>> = _userProfileState

    private val _updateProfileState = MutableLiveData<Resource<Unit>>()
    val updateProfileState: LiveData<Resource<Unit>> = _updateProfileState

    private val _uploadImageState = MutableLiveData<Resource<String>>()
    val uploadImageState: LiveData<Resource<String>> = _uploadImageState

    val currentUserId: String
        get() = authRepository.currentUserId

    fun loadUserProfile() {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _userProfileState.value = Resource.Error("User not authenticated")
            return
        }
        userRepository.fetchUserProfile(userId) { result ->
            _userProfileState.value = result
        }
    }

    fun updateUserProfile(displayName: String, email: String, profilePictureUrl: String) {
        val userId = authRepository.currentUserId
        if (userId.isEmpty()) {
            _updateProfileState.value = Resource.Error("User not authenticated")
            return
        }
        userRepository.updateUserProfile(userId, displayName, email, profilePictureUrl) { result ->
            _updateProfileState.value = result
            if (result is Resource.Success) {
                loadUserProfile()
            }
        }
    }

    fun uploadProfilePicture(imageUri: Uri) {
        userRepository.uploadProfilePicture(imageUri) { result ->
            _uploadImageState.value = result
        }
    }
}
