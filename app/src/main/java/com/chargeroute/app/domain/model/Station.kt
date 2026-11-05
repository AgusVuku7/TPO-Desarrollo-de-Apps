package com.chargeroute.app.domain.model

data class Station(
    val id: String,
    val remoteId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val connectors: List<Connector>,
    val availableCount: Int = 2,
    val totalCount: Int = 4,
    val amenities: List<String> = listOf("24 h", "Café", "Baños", "Wi-Fi"),
    val operator: String = "YPF Punto Eléctrico",
    val notes: String = "Sin reserva previa. Confirmá tarifa y acceso antes de viajar.",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    fun isCompatibleWith(vehicleConnectors: List<ConnectorType>): Boolean {
        return connectors.any { it.type in vehicleConnectors }
    }
}
