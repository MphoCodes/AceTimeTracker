package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.ui.screen.RegistrationScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.AuthViewModel
import com.example.acetimetracker.util.Resource

class RegistrationActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModel()

        setContent {
            AceTimeTrackerTheme {
                val registerState by viewModel.registerState.observeAsState()

                RegistrationScreen(
                    registerState = registerState,
                    onRegisterClick = { email, password, confirm ->
                        viewModel.register(email, password, confirm)
                    },
                    onTestLoginClick = {
                        viewModel.loginTestAccount()
                    },
                    onNavigateToLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Logged in with test account!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
