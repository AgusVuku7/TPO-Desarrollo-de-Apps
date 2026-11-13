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
 * Elegir la mas lejana suele minimizar la cantidad de paradas.
 *
 * Excepcion: si un poco antes de la mas lejana hay un cargador claramente mas rapido
 * para este auto, se para en ese, porque se pierde menos tiempo cargando.
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

    // Desde este porcentaje la carga se hace mas lenta (a la mitad de potencia)
    const val SLOW_CHARGE_FROM_PERCENT = 80.0
    const val SLOW_CHARGE_FACTOR = 0.5

    // Cuantos km antes de la estacion mas lejana se busca un cargador mas rapido
    const val FAST_CHARGER_WINDOW_KM = 50.0

    // Solo se cambia de estacion si la otra carga al menos 1,5 veces mas rapido
    const val FAST_CHARGER_MIN_GAIN = 1.5

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
            val farthest = reachable.maxByOrNull { it.km } ?: break // no hay ninguna estacion alcanzable
            val next = fasterNearby(farthest, reachable, vehicle) ?: farthest

            val arrival = arrivalAt(next)
            val neededToFinish = percentUsed(routeKm - next.km) + SAFETY_RESERVE_PERCENT
            val departure = neededToFinish.coerceIn(maxOf(MIN_CHARGE_PERCENT, arrival), 100.0)

            val energyKwh = (departure - arrival) / 100.0 * vehicle.usableBatteryKwh
            val stationKw = knownPowerKw(next.station, vehicle) ?: DEFAULT_STATION_KW
            val chargeKw = minOf(stationKw, vehicle.maxDcKw)
            val minutes = chargeMinutes(arrival, departure, vehicle.usableBatteryKwh, chargeKw)

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

    /**
     * Minutos para cargar desde fromPercent hasta toPercent.
     * En carga rapida la bateria acepta menos potencia cuando esta casi llena:
     * hasta el 80 % se carga a la potencia indicada y del 80 % en adelante a la mitad.
     */
    fun chargeMinutes(fromPercent: Double, toPercent: Double, batteryKwh: Double, powerKw: Double): Int {
        if (toPercent <= fromPercent) return 0
        val fastPart = (minOf(toPercent, SLOW_CHARGE_FROM_PERCENT) - fromPercent).coerceAtLeast(0.0)
        val slowPart = (toPercent - maxOf(fromPercent, SLOW_CHARGE_FROM_PERCENT)).coerceAtLeast(0.0)

        val fastHours = fastPart / 100.0 * batteryKwh / powerKw
        val slowHours = slowPart / 100.0 * batteryKwh / (powerKw * SLOW_CHARGE_FACTOR)
        // Se redondea para arriba; el 0.0001 evita que un error de decimales (36,0000001) sume un minuto
        return ceil((fastHours + slowHours) * 60.0 - 0.0001).toInt().coerceAtLeast(5)
    }

    /**
     * Busca, entre las estaciones alcanzables que estan hasta FAST_CHARGER_WINDOW_KM antes
     * de la mas lejana, la que carga mas rapido a este auto.
     * Devuelve null si ninguna es claramente mas rapida que la mas lejana.
     */
    private fun fasterNearby(
        farthest: StationOnRoute,
        reachable: List<StationOnRoute>,
        vehicle: Vehicle
    ): StationOnRoute? {
        // Se ordenan de la mas lejana a la mas cercana: si dos empatan en potencia, queda la mas lejana
        val fastest = reachable
            .filter { farthest.km - it.km <= FAST_CHARGER_WINDOW_KM }
            .sortedByDescending { it.km }
            .maxByOrNull { usefulKw(it.station, vehicle) } ?: return null

        val fastestKw = usefulKw(fastest.station, vehicle)
        val farthestKw = usefulKw(farthest.station, vehicle)
        val isClearlyFaster = fastestKw > farthestKw && fastestKw >= farthestKw * FAST_CHARGER_MIN_GAIN
        return if (isClearlyFaster) fastest else null
    }

    // Potencia que el auto puede aprovechar en esa estacion: la del cargador, con el tope del auto.
    // Para comparar, una estacion sin dato de potencia vale 0: no le gana a una con dato real.
    private fun usefulKw(station: Station, vehicle: Vehicle): Double =
        minOf(knownPowerKw(station, vehicle) ?: 0.0, vehicle.maxDcKw)

    // Potencia del mejor conector compatible con el auto (null si la estacion no la informa)
    private fun knownPowerKw(station: Station, vehicle: Vehicle): Double? =
        station.connectors
            .filter { it.type in vehicle.supportedConnectors && it.powerKw > 0 }
            .maxOfOrNull { it.powerKw }
}
