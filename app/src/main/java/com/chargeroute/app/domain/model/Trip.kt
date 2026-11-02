package com.chargeroute.app.domain.model

data class Trip(
    val id: String,
    val origin: LocationPoint,
    val destination: LocationPoint,
    val vehicle: Vehicle,
    val initialBatteryPercent: Int,
    val currentBatteryPercent: Int,
    val acActive: Boolean,
    val routePlan: RoutePlan,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
