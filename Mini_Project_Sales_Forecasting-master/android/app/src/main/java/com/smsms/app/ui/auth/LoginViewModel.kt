package com.smsms.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsms.app.repository.AuthRepository
import com.smsms.app.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object LoggingIn : LoginUiState()
    data object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Username and password are required.")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.LoggingIn
            when (val result = AuthRepository.login(username, password)) {
                is ApiResult.Success -> _uiState.value = LoginUiState.Success
                is ApiResult.Failure -> _uiState.value = LoginUiState.Error(result.message)
            }
        }
    }
}
