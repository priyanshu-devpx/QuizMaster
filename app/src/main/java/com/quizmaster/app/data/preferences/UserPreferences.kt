package com.quizmaster.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferences(
    private val context: Context
) {

    companion object {
        private val NOTIFICATIONS_ENABLED =
            booleanPreferencesKey("notifications_enabled")

        private val DARK_MODE_ENABLED =
            booleanPreferencesKey("dark_mode_enabled")
    }

    val notificationsEnabled: Flow<Boolean> =
        context.userPreferencesDataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }

    val darkModeEnabled: Flow<Boolean> =
        context.userPreferencesDataStore.data.map { preferences ->
            preferences[DARK_MODE_ENABLED] ?: false
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[NOTIFICATIONS_ENABLED] = enabled
            }
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[DARK_MODE_ENABLED] = enabled
            }
        }
    }
}