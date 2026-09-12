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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

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
    private val googleSignInInProgress = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            tokenManager.clearLegacyToken()
        }
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
                    val tokenResult = currentUser.getIdToken(true).await()
                    val token = tokenResult.token ?: return@launch
                    _uiState.value = _uiState.value.copy(isLoading = true)
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
                val token = authResult.user?.getIdToken(true)?.await()?.token ?: throw Exception("Token nullo")
                _uiState.value = _uiState.value.copy(isLoading = true)
                fetchProfile(token)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Errore di autenticazione")
            }
        }
    }

    fun signInWithGoogle(context: Context, completeOnboardingOnSuccess: Boolean = false) {
        if (_uiState.value.isLoading || !googleSignInInProgress.compareAndSet(false, true)) {
            Log.w("AuthViewModel", "Tentativo Google ignorato: autenticazione già in corso")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
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
                if (result.credential !is CustomCredential || result.credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    throw IllegalStateException("Credenziale Google non valida")
                }

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                val firebaseToken = authResult.user?.getIdToken(true)?.await()?.token ?: throw Exception("Token Firebase nullo")
                fetchProfile(firebaseToken, completeOnboardingOnSuccess)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Accesso Google annullato o non riuscito.")
            } finally {
                googleSignInInProgress.set(false)
            }
        }
    }
    private suspend fun fetchProfile(token: String, completeOnboardingOnSuccess: Boolean = false) {
        try {
            val profile = apiService.getMyProfile("Bearer $token")
            if (completeOnboardingOnSuccess) {
                // La landing termina soltanto dopo Firebase + backend validi.
                tokenManager.setHasSeenOnboarding(true)
            }
            _uiState.value = _uiState.value.copy(isLoggedIn = true, userProfile = profile, isLoading = false, errorMessage = null)
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
                _uiState.value = _uiState.value.copy(isLoading = true)
                val tokenResult = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()
                val token = tokenResult?.token ?: return@launch
                
                val request = UpdatePreferencesRequest(team, driver1, driver2, true)
                val updatedProfile = apiService.updatePreferences("Bearer $token", request)
                _uiState.value = _uiState.value.copy(userProfile = updatedProfile, isLoading = false)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Errore aggiornamento preferenze", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
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