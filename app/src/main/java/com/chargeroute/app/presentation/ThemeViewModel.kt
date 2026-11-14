package com.chargeroute.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.ThemeMode
import com.chargeroute.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tema elegido en el menu (Claro, Oscuro, Auto o el del sistema), guardado en el repositorio de usuario.
 * Asi la Activity no le habla directo a la capa de datos.
 */
class ThemeViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.LIGHT)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userRepository.setThemeMode(mode) }
    }
}
