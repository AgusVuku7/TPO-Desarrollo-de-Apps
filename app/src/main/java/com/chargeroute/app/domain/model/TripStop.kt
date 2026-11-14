package com.chargeroute.app.domain.model

data class TripStop(
    val id: String,
    val station: Station,
    val orderIndex: Int,
    val stopDurationMinutes: Int,
    val arrivalBatteryPercent: Int,
    val departureBatteryPercent: Int,
    val chargedEnergyKwh: Double,
    val arrivalTime: String = "",
    val departureTime: String = ""
)
