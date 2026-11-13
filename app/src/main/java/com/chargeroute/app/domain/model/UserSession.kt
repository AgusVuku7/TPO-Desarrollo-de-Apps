package com.chargeroute.app.domain.model

// Datos del usuario que inicio sesion. El invitado no tiene sesion: nombre y mail quedan vacios
data class UserSession(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = ""
)

/**
 * Validaciones del formulario de login.
 * Es Kotlin puro (sin Android), asi se prueba con tests unitarios.
 */
object ProfileValidator {

    // Algo antes de la arroba, algo despues y un punto en el dominio (ej. ana@mail.com)
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidName(name: String): Boolean = name.trim().isNotEmpty()

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
}
