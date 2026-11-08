package com.chargeroute.app.domain.model

// Un cargador donde se paro en algun viaje guardado
data class UsedCharger(
    val station: Station,
    val tripsCount: Int,
    // Datos del viaje mas reciente que paso por este cargador (para abrir su detalle)
    val lastTripId: String,
    val lastStopId: String,
    val lastTripLabel: String,
    val lastUsedMillis: Long
)

object UsedChargers {

    // Junta las paradas de todos los viajes guardados, sin repetir cargadores.
    // Primero los usados mas recientemente.
    fun fromTrips(trips: List<Trip>): List<UsedCharger> =
        trips.sortedByDescending { it.createdAtMillis }
            .flatMap { trip -> trip.routePlan.stops.map { stop -> trip to stop } }
            .groupBy { (_, stop) -> stop.station.id }
            .values
            .map { uses ->
                val (lastTrip, lastStop) = uses.first()
                UsedCharger(
                    station = lastStop.station,
                    tripsCount = uses.map { it.first.id }.distinct().size,
                    lastTripId = lastTrip.id,
                    lastStopId = lastStop.id,
                    lastTripLabel = tripLabel(lastTrip),
                    lastUsedMillis = lastTrip.createdAtMillis
                )
            }

    private fun tripLabel(trip: Trip): String =
        "${trip.origin.name.split(",").first()} → ${trip.destination.name.split(",").first()}"
}
