package com.chargeroute.app.domain.model

import kotlin.math.ceil

// Una estacion y el kilometro de la ruta en el que queda
data class StationOnRoute(val station: Station, val km: Double)

// Una parada de carga decidida por el planificador
data class PlannedStop(
    val station: Station,
    val km: Double,
    val arrivalBatteryPercent: Int,
    val departureBatteryPercent: Int,
    val chargedEnergyKwh: Double,
    val chargeMinutes: Int
)

data class ChargingPlan(
    val stops: List<PlannedStop>,
    val arrivalBatteryPercent: Int,
    val reachesDestination: Boolean // false si con las estaciones encontradas no se llega
)

/**
 * Regla de negocio principal: decide en que estaciones parar a cargar.
 *
 * Algoritmo (greedy): mientras la bateria no alcance para llegar al destino con reserva,
 * se elige la estacion MAS LEJANA a la que se llega, se carga y se repite desde ahi.
 * Elegir siempre la mas lejana minimiza la cantidad de paradas.
 *
 * Es Kotlin puro (sin Android ni red), asi se prueba con tests unitarios.
 */
object ChargingPlanner {

    // Margen de seguridad sugerido en la preentrega: llegar con al menos 15 %
    const val SAFETY_RESERVE_PERCENT = 15.0

    // En cada parada se carga por lo menos hasta este porcentaje
    const val MIN_CHARGE_PERCENT = 80.0

    // Potencia que se asume si la estacion no la informa
    const val DEFAULT_STATION_KW = 50.0

    // Tope para que nunca quede en un ciclo infinito
    private const val MAX_STOPS = 10

    fun plan(
        routeKm: Double,
        stations: List<StationOnRoute>,
        vehicle: Vehicle,
        consumptionKwhPer100Km: Double,
        initialBatteryPercent: Int
    ): ChargingPlan {
        // Porcentaje de bateria que se gasta en recorrer esos km
        fun percentUsed(km: Double): Double =
            km * consumptionKwhPer100Km / 100.0 / vehicle.usableBatteryKwh * 100.0

        val candidates = stations.filter { it.km > 0.0 && it.km < routeKm }.sortedBy { it.km }
        val stops = mutableListOf<PlannedStop>()
        var currentKm = 0.0
        var battery = initialBatteryPercent.toDouble()

        while (stops.size < MAX_STOPS) {
            // Si ya se llega al destino con reserva, no hacen falta mas paradas
            if (battery - percentUsed(routeKm - currentKm) >= SAFETY_RESERVE_PERCENT) break

            val ahead = candidates.filter { it.km > currentKm + 1.0 }
            fun arrivalAt(candidate: StationOnRoute) = battery - percentUsed(candidate.km - currentKm)

            // Primero las que se alcanzan con reserva; si no hay, las que se alcanzan aunque sea justo
            val withReserve = ahead.filter { arrivalAt(it) >= SAFETY_RESERVE_PERCENT }
            val reachable = withReserve.ifEmpty { ahead.filter { arrivalAt(it) >= 0.0 } }
            val next = reachable.maxByOrNull { it.km } ?: break // no hay ninguna estacion alcanzable

            val arrival = arrivalAt(next)
            val neededToFinish = percentUsed(routeKm - next.km) + SAFETY_RESERVE_PERCENT
            val departure = neededToFinish.coerceIn(maxOf(MIN_CHARGE_PERCENT, arrival), 100.0)

            val energyKwh = (departure - arrival) / 100.0 * vehicle.usableBatteryKwh
            val chargeKw = minOf(stationPowerKw(next.station, vehicle), vehicle.maxDcKw)
            val minutes = ceil(energyKwh / chargeKw * 60.0).toInt().coerceAtLeast(5)

            stops.add(
                PlannedStop(
                    station = next.station,
                    km = next.km,
                    arrivalBatteryPercent = arrival.toInt(),
                    departureBatteryPercent = departure.toInt(),
                    chargedEnergyKwh = (energyKwh * 10.0).toInt() / 10.0,
                    chargeMinutes = minutes
                )
            )
            currentKm = next.km
            battery = departure
        }

        val finalBattery = battery - percentUsed(routeKm - currentKm)
        return ChargingPlan(
            stops = stops,
            arrivalBatteryPercent = finalBattery.toInt().coerceIn(0, 100),
            reachesDestination = finalBattery >= 0.0
        )
    }

    // Ubica cada estacion sobre la ruta (en que km queda)
    fun locateOnRoute(
        stations: List<Station>,
        route: List<Pair<Double, Double>>,
        routeKm: Double
    ): List<StationOnRoute> = stations.map { station ->
        val match = RouteGeometry.nearestOnRoute(route, station.latitude, station.longitude)
        StationOnRoute(station, match.progress * routeKm)
    }

    // Potencia del mejor conector compatible con el auto (o la potencia por defecto)
    private fun stationPowerKw(station: Station, vehicle: Vehicle): Double =
        station.connectors
            .filter { it.type in vehicle.supportedConnectors && it.powerKw > 0 }
            .maxOfOrNull { it.powerKw } ?: DEFAULT_STATION_KW
}
