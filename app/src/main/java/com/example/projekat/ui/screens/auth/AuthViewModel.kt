package com.example.projekat.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.repository.AuthRepository
import com.example.projekat.data.repository.AuthResult
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form fields for login/register
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    init {
        // Observe auth state
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = user != null,
                    user = user
                )
            }
        }
    }

    fun updateEmail(value: String) {
        _email.value = value
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun updateConfirmPassword(value: String) {
        _confirmPassword.value = value
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun login() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value

        // Validate
        if (emailValue.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Unesite email adresu")
            return
        }
        if (passwordValue.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Unesite lozinku")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = authRepository.login(emailValue, passwordValue)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = result.user
                    )
                    clearFields()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun register() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value
        val confirmPasswordValue = _confirmPassword.value

        // Validate
        if (emailValue.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Unesite email adresu")
            return
        }
        if (passwordValue.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Unesite lozinku")
            return
        }
        if (passwordValue.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Lozinka mora imati najmanje 6 karaktera")
            return
        }
        if (passwordValue != confirmPasswordValue) {
            _uiState.value = _uiState.value.copy(error = "Lozinke se ne poklapaju")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = authRepository.register(emailValue, passwordValue)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = result.user
                    )
                    clearFields()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }

    fun sendPasswordReset() {
        val emailValue = _email.value.trim()

        if (emailValue.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Unesite email adresu")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.sendPasswordResetEmail(emailValue).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    private fun clearFields() {
        _email.value = ""
        _password.value = ""
        _confirmPassword.value = ""
    }
}
