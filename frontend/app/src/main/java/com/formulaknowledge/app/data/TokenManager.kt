package com.formulaknowledge.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        // Chiave mantenuta solo per eliminare il token salvato dalle versioni precedenti.
        private val LEGACY_JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
    }

    val hasSeenOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    suspend fun setHasSeenOnboarding(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = value
        }
    }

    suspend fun clearLegacyToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(LEGACY_JWT_TOKEN_KEY)
        }
    }
}