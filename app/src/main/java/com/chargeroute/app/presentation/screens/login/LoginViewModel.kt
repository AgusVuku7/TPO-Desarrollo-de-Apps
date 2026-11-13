package com.chargeroute.app.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.data.local.datastore.UserPreferencesDataStore
import com.chargeroute.app.domain.model.ProfileValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Los errores son Boolean: el texto lo elige la pantalla, porque el ViewModel no tiene Context
data class LoginUiState(
    val name: String = "",
    val email: String = "",
    val nameError: Boolean = false,
    val emailError: Boolean = false
)

/**
 * Login simulado: no hay servidor ni contraseña. Valida el formulario y guarda
 * el perfil (nombre y mail) en DataStore.
 */
class LoginViewModel(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Evento de una sola vez: avisa que el perfil ya se guardo y se puede navegar
    private val _loginDone = MutableSharedFlow<Unit>()
    val loginDone: SharedFlow<Unit> = _loginDone.asSharedFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = false) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = false) }
    }

    fun login() {
        val name = _uiState.value.name.trim()
        val email = _uiState.value.email.trim()
        val nameOk = ProfileValidator.isValidName(name)
        val emailOk = ProfileValidator.isValidEmail(email)

        if (!nameOk || !emailOk) {
            _uiState.update { it.copy(nameError = !nameOk, emailError = !emailOk) }
            return
        }

        // Primero se guarda y recien despues se avisa, para no navegar con el perfil a medio guardar
        viewModelScope.launch {
            userPreferences.login(name, email)
            _loginDone.emit(Unit)
        }
    }
}
