package com.chargeroute.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.data.local.datastore.UserPreferencesDataStore
import com.chargeroute.app.domain.model.UserSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sesion del usuario guardada en DataStore. La usa la navegacion para decidir en que
 * pantalla arrancar y el menu lateral para mostrar el nombre y el mail.
 */
class SessionViewModel(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    // null = todavia no se termino de leer DataStore
    val session: StateFlow<UserSession?> = userPreferences.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() {
        viewModelScope.launch { userPreferences.logout() }
    }
}
