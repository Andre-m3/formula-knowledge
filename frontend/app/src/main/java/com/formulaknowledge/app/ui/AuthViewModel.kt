package com.formulaknowledge.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.formulaknowledge.app.data.F1ApiService
import com.formulaknowledge.app.data.RetrofitClient
import com.formulaknowledge.app.data.TokenManager
import com.formulaknowledge.app.data.UserLoginRequest
import com.formulaknowledge.app.data.UserProfileResponse
import com.formulaknowledge.app.data.UserRegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userProfile: UserProfileResponse? = null,
    val hasSeenOnboarding: Boolean = false,
    val isCheckingOnboarding: Boolean = true
)

class AuthViewModel(
    private val apiService: F1ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            tokenManager.hasSeenOnboardingFlow.collect { hasSeen ->
                _uiState.value = _uiState.value.copy(
                    hasSeenOnboarding = hasSeen,
                    isCheckingOnboarding = false
                )
            }
        }
        checkSavedToken()
    }

    private fun checkSavedToken() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.firstOrNull()
            if (!token.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = true)
                fetchProfile(token)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            tokenManager.setHasSeenOnboarding(true)
        }
    }

    fun authenticate(email: String, pass: String, isRegister: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = if (isRegister) {
                    apiService.register(UserRegisterRequest(email, pass, full_name = "Tifoso"))
                } else {
                    apiService.login(UserLoginRequest(email, pass))
                }
                tokenManager.saveToken(response.access_token)
                _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = false)
                fetchProfile(response.access_token)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = if (isRegister) "Registrazione fallita." else "Credenziali errate.")
            }
        }
    }

    private suspend fun fetchProfile(token: String) {
        try {
            val profile = apiService.getMyProfile("Bearer $token")
            _uiState.value = _uiState.value.copy(userProfile = profile, isLoading = false)
        } catch (e: Exception) {
            logout() // Se il token è scaduto, sloggiamo l'utente
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            _uiState.value = AuthUiState() // Reset dello stato a "Sloggato"
        }
    }
}

// Factory per permettere al ViewModel di ricevere il Context per il DataStore
class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val tokenManager = TokenManager(context)
        return AuthViewModel(RetrofitClient.apiService, tokenManager) as T
    }
}