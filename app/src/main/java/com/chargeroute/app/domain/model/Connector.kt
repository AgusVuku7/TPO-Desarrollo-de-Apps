package com.chargeroute.app.domain.model

enum class ConnectorType(val displayName: String, val category: String) {
    CCS2("CCS2", "DC Carga rápida"),
    TYPE_2("Type 2 (Mennekes)", "AC Carga semi-rápida"),
    CHADEMO("CHAdeMO", "DC Carga rápida"),
    CCS1("CCS1", "DC Carga rápida"),
    TESLA("Tesla Supercharger", "DC Propietario"),
    SCHUKO("Doméstico (Schuko)", "AC Carga lenta");

    companion object {
        fun fromString(name: String): ConnectorType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.contains(name, ignoreCase = true) } ?: CCS2
        }
    }
}

data class Connector(
    val type: ConnectorType,
    val powerKw: Double,
    val isCompatible: Boolean = true
)
