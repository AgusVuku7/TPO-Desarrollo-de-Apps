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
    // Momento en que se calculo la ruta: se toma como hora de salida
    val calculatedAtMillis: Long = System.currentTimeMillis(),
    val departureTime: String = "",
    val arrivalTime: String = "",
    val isLowBatteryWarning: Boolean = false,
    val warningMessage: String? = null,
    val isApproximate: Boolean = false, // true si no se pudo consultar el camino real
    // Distancia del camino directo, sin pasar por los cargadores (0 = sin dato)
    val directDistanceKm: Double = 0.0
) {
    // Km extra por salir de la ruta para ir a cargar
    val detourKm: Int
        get() = if (directDistanceKm > 0) (distanceKm - directDistanceKm).toInt().coerceAtLeast(0) else 0
}
