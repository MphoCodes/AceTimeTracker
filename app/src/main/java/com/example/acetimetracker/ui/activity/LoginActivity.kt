package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.ui.screen.LoginScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.AuthViewModel
import com.example.acetimetracker.util.Resource

class LoginActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModel()

        setContent {
            AceTimeTrackerTheme {
                val loginState by viewModel.loginState.observeAsState()

                LoginScreen(
                    loginState = loginState,
                    onLoginClick = { email, password ->
                        viewModel.login(email, password)
                    },
                    onTestLoginClick = {
                        viewModel.loginTestAccount()
                    },
                    onForgotPasswordClick = { email ->
                        viewModel.sendPasswordReset(email)
                    },
                    onNavigateToRegister = {
                        startActivity(Intent(this, RegistrationActivity::class.java))
                    }
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.passwordResetState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
