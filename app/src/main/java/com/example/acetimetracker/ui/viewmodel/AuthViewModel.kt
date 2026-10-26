package com.example.acetimetracker.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.util.Resource
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<FirebaseUser>>()
    val loginState: LiveData<Resource<FirebaseUser>> = _loginState

    private val _registerState = MutableLiveData<Resource<FirebaseUser>>()
    val registerState: LiveData<Resource<FirebaseUser>> = _registerState

    private val _passwordResetState = MutableLiveData<Resource<Unit>>()
    val passwordResetState: LiveData<Resource<Unit>> = _passwordResetState

    val isUserLoggedIn: Boolean
        get() = authRepository.isAuthenticated

    val currentUserId: String
        get() = authRepository.currentUserId

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Please fill in all fields")
            return
        }
        authRepository.login(email.trim(), password.trim()) { result ->
            _loginState.value = result
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _registerState.value = Resource.Error("Please fill in all fields")
            return
        }
        if (password != confirmPassword) {
            _registerState.value = Resource.Error("Passwords do not match")
            return
        }
        authRepository.register(email.trim(), password.trim()) { result ->
            _registerState.value = result
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _passwordResetState.value = Resource.Error("Please enter your email address")
            return
        }
        authRepository.sendPasswordReset(email.trim()) { result ->
            _passwordResetState.value = result
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
