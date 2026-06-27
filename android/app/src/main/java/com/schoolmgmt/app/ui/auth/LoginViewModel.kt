package com.schoolmgmt.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.repository.AuthRepository
import com.schoolmgmt.app.data.repository.LoginFailedException
import com.schoolmgmt.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    /**
     * onSuccess is a plain callback (not a Flow/state value) because
     * navigation is a one-time event, not persistent UI state — if we
     * stored "loggedIn = true" in uiState instead, rotating the screen
     * or recomposing could re-trigger navigation unexpectedly.
     */
    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Enter both username and password")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                authRepository.login(state.username.trim(), state.password)
                // Kick off an immediate sync right after login so the
                // person's data starts populating without waiting for
                // the next periodic window — see SyncScheduler doc comment.
                syncScheduler.schedulePeriodicSync()
                syncScheduler.syncNow()
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: LoginFailedException) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Something went wrong: ${e.message ?: "please try again"}",
                )
            }
        }
    }
}
