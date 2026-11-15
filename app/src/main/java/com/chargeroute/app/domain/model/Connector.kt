package com.chargeroute.app.domain.model

// isDc: true si es corriente continua (carga rapida), false si es alterna
enum class ConnectorType(val displayName: String, val isDc: Boolean) {
    CCS2("CCS2", true),
    TYPE_2("Type 2 (Mennekes)", false),
    CHADEMO("CHAdeMO", true),
    CCS1("CCS1", true),
    TESLA("Tesla Supercharger", true),
    SCHUKO("Doméstico (Schuko)", false);

    companion object {
        // Devuelve null si el texto no coincide con ningun conector conocido
        fun fromString(name: String): ConnectorType? {
            if (name.isEmpty()) return null
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.contains(name, ignoreCase = true) }
        }
    }
}

data class Connector(
    val type: ConnectorType,
    val powerKw: Double,
    val isCompatible: Boolean = true
)
