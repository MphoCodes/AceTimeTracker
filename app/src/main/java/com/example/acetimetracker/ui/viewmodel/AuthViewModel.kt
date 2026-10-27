package com.example.acetimetracker.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acetimetracker.data.model.AuthUser
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.util.Resource

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<AuthUser>>()
    val loginState: LiveData<Resource<AuthUser>> = _loginState

    private val _registerState = MutableLiveData<Resource<AuthUser>>()
    val registerState: LiveData<Resource<AuthUser>> = _registerState

    private val _passwordResetState = MutableLiveData<Resource<Unit>>()
    val passwordResetState: LiveData<Resource<Unit>> = _passwordResetState

    val isUserLoggedIn: Boolean
        get() = authRepository.isAuthenticated

    val currentUserId: String
        get() = authRepository.currentUserId

    fun loginTestAccount() {
        authRepository.loginTestAccount { result ->
            _loginState.value = result
        }
    }

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
