package com.chargeroute.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chargeroute.app.core.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val context: Context) {

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_SELECTED_VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
    private val KEY_IS_LOGGED_IN = stringPreferencesKey("is_logged_in")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    val selectedVehicleId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_VEHICLE_ID] ?: "hyundai_kona_2023"
    }

    suspend fun setSelectedVehicleId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_VEHICLE_ID] = id
        }
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: "Sofía Martín"
    }

    val userEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL] ?: "sofia.martin@gmail.com"
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        (preferences[KEY_IS_LOGGED_IN] ?: "true") == "true"
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean, name: String = "Sofía Martín", email: String = "sofia.martin@gmail.com") {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = isLoggedIn.toString()
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_EMAIL] = email
        }
    }
}
