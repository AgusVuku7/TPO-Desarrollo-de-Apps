package com.chargeroute.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.repository.UserRepository
import com.chargeroute.app.domain.model.UserSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sesion del usuario (viene del repositorio de usuario). La usa la navegacion para decidir en que
 * pantalla arrancar y el menu lateral para mostrar el nombre y el mail.
 */
class SessionViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    // null = todavia no se termino de leer la sesion
    val session: StateFlow<UserSession?> = userRepository.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() {
        viewModelScope.launch { userRepository.logout() }
    }
}
