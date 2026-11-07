package com.chargeroute.app.domain.model

data class RoutePlan(
    val origin: LocationPoint,
    val destination: LocationPoint,
    val distanceKm: Double,
    val totalDurationMinutes: Int,
    val driveDurationMinutes: Int,
    val chargeDurationMinutes: Int,
    val stops: List<TripStop>,
    // RF02: estaciones compatibles cerca de la ruta (se muestran en el mapa).
    // No se guardan en la tabla de rutas: al abrir un viaje se recalculan con las estaciones de Room.
    val nearbyStations: List<Station> = emptyList(),
    val polylinePoints: List<Pair<Double, Double>>,
    val encodedPolyline: String,
    val arrivalBatteryPercent: Int,
    val consumptionKwhPer100Km: Double,
    val departureTime: String = "09:41",
    val arrivalTime: String = "15:26",
    val isLowBatteryWarning: Boolean = false,
    val warningMessage: String? = null,
    val isApproximate: Boolean = false // true si no se pudo consultar el camino real
)
