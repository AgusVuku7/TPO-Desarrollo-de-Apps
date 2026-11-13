package com.chargeroute.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chargeroute.app.core.theme.ThemeMode
import com.chargeroute.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val context: Context) {

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

    // Sesion del usuario: si no hay nada guardado, no hay sesion iniciada
    val session: Flow<UserSession> = context.dataStore.data.map { preferences ->
        UserSession(
            isLoggedIn = preferences[KEY_IS_LOGGED_IN] ?: false,
            userName = preferences[KEY_USER_NAME] ?: "",
            userEmail = preferences[KEY_USER_EMAIL] ?: ""
        )
    }

    suspend fun login(name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_EMAIL] = email
        }
    }

    // Borra solo el perfil: el tema y el auto elegido se mantienen
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_IS_LOGGED_IN)
            preferences.remove(KEY_USER_NAME)
            preferences.remove(KEY_USER_EMAIL)
        }
    }

    // Claves de cada preferencia guardada (se crean una sola vez para toda la app)
    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_SELECTED_VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
}
