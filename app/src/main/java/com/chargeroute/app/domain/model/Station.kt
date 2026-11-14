package com.chargeroute.app.domain.model

data class Station(
    val id: String,
    val remoteId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val connectors: List<Connector>,
    val availableCount: Int = 0,
    val totalCount: Int = 0,
    val amenities: List<String> = emptyList(),
    val operator: String = "",
    val notes: String = "",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    fun isCompatibleWith(vehicleConnectors: List<ConnectorType>): Boolean {
        return connectors.any { it.type in vehicleConnectors }
    }
}
