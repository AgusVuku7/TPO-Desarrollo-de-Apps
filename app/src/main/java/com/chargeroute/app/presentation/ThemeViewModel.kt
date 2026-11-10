package com.chargeroute.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.core.theme.ThemeMode
import com.chargeroute.app.data.local.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tema elegido en el menu (Claro, Oscuro, Auto o el del sistema), guardado en DataStore.
 * Asi la Activity no le habla directo a DataStore.
 */
class ThemeViewModel(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }
}
