package com.formulaknowledge.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.formulaknowledge.app.data.F1ApiService
import retrofit2.HttpException
import android.util.Log
import com.formulaknowledge.app.data.RetrofitClient
import com.formulaknowledge.app.data.UpdatePreferencesRequest
import com.formulaknowledge.app.data.TokenManager
import com.formulaknowledge.app.data.UserProfileResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
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
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                try {
                    val tokenResult = currentUser.getIdToken(false).await()
                    val token = tokenResult.token ?: return@launch
                    tokenManager.saveToken(token) // Lo salviamo localmente per le API
                    _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = true)
                    fetchProfile(token)
                } catch (e: Exception) {
                    logout()
                }
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
                val auth = FirebaseAuth.getInstance()
                val authResult = if (isRegister) {
                    auth.createUserWithEmailAndPassword(email, pass).await()
                } else {
                    auth.signInWithEmailAndPassword(email, pass).await()
                }
                val token = authResult.user?.getIdToken(false)?.await()?.token ?: throw Exception("Token nullo")
                tokenManager.saveToken(token)
                _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = false)
                fetchProfile(token)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Errore di autenticazione")
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val credentialManager = CredentialManager.create(context)
                val webClientId = context.getString(context.resources.getIdentifier("default_web_client_id", "string", context.packageName))
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()
                    
                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result = credentialManager.getCredential(context, request)
                
                if (result.credential is CustomCredential && result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    
                    val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                    val firebaseToken = authResult.user?.getIdToken(false)?.await()?.token ?: throw Exception("Token Firebase nullo")
                    
                    tokenManager.saveToken(firebaseToken)
                    _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = false)
                    fetchProfile(firebaseToken)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Accesso Google annullato o non riuscito.")
            }
        }
    }

    private suspend fun fetchProfile(token: String) {
        try {
            val profile = apiService.getMyProfile("Bearer $token")
            _uiState.value = _uiState.value.copy(userProfile = profile, isLoading = false)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Errore nel caricamento del profilo", e)
            if (e is HttpException && e.code() == 401) {
                logout() // Sloggiamo solo se il token è effettivamente invalido/scaduto (401 Unauthorized)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Profilo non caricato")
            }
        }
    }

    fun updatePreferences(driver1: String?, driver2: String?, team: String?) {
        viewModelScope.launch {
            try {
                val tokenResult = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()
                val token = tokenResult?.token ?: return@launch
                
                val request = UpdatePreferencesRequest(team, driver1, driver2, true)
                val updatedProfile = apiService.updatePreferences("Bearer $token", request)
                _uiState.value = _uiState.value.copy(userProfile = updatedProfile)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Errore aggiornamento preferenze", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            tokenManager.clearToken()
            _uiState.value = AuthUiState(hasSeenOnboarding = _uiState.value.hasSeenOnboarding, isCheckingOnboarding = false)
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