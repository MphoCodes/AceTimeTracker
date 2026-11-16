package com.mphocodes.acetimetracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mphocodes.acetimetracker.data.model.UserProfile
import com.mphocodes.acetimetracker.data.repository.AuthRepository
import com.mphocodes.acetimetracker.data.repository.UserRepository
import com.mphocodes.acetimetracker.util.Resource

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val imageUploader: (Uri, (Resource<String>) -> Unit) -> Unit = userRepository::uploadProfilePicture
) : ViewModel() {

    private val _userProfileState = MutableLiveData<Resource<UserProfile>>()
    val userProfileState: LiveData<Resource<UserProfile>> = _userProfileState

    private val _updateProfileState = MutableLiveData<Resource<Unit>>()
    val updateProfileState: LiveData<Resource<Unit>> = _updateProfileState

    private val _uploadImageState = MutableLiveData<Resource<String>>()
    val uploadImageState: LiveData<Resource<String>> = _uploadImageState

    private var imageUploadGeneration = 0L

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
        if (_uploadImageState.value is Resource.Loading) {
            _updateProfileState.value = Resource.Error("Please wait for the photo to finish uploading")
            return
        }
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
        val generation = ++imageUploadGeneration
        if (authRepository.isMockSession) {
            _uploadImageState.value = Resource.Success(imageUri.toString())
            return
        }
        imageUploader(imageUri) { result ->
            if (generation == imageUploadGeneration) {
                _uploadImageState.value = result
            }
        }
    }
}
