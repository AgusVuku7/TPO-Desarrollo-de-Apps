package com.chargeroute.app.domain.model

data class Vehicle(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val version: String,
    val usableBatteryKwh: Double,
    val wltpRangeKm: Double,
    val supportedConnectors: List<ConnectorType>,
    val maxDcKw: Double,
    val baseConsumptionKwhPer100Km: Double = (usableBatteryKwh / wltpRangeKm) * 100.0
) {
    val displayName: String
        get() = "$brand $model ($version)"
}
