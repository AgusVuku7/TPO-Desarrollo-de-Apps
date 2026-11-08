package com.chargeroute.app.data.repository

import com.chargeroute.app.core.network.NetworkConnectivityObserver
import com.chargeroute.app.data.local.ChargeRouteDatabase
import com.chargeroute.app.data.mapper.RouteMapper
import com.chargeroute.app.data.mapper.StationMapper
import com.chargeroute.app.data.remote.RetrofitClient
import com.chargeroute.app.data.remote.dto.OsrmRoute
import com.chargeroute.app.data.remote.dto.OsrmStep
import com.chargeroute.app.domain.model.NavStep
import com.chargeroute.app.domain.model.ChargingPlanner
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.NoConnectionException
import com.chargeroute.app.domain.model.RouteServiceException
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.TripClock
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.RouteRepository
import com.chargeroute.app.domain.repository.StationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class RouteRepositoryImpl(
    private val database: ChargeRouteDatabase,
    private val networkConnectivityObserver: NetworkConnectivityObserver,
    private val stationRepository: StationRepository
) : RouteRepository {

    private val osrmApi = RetrofitClient.osrmApi

    override suspend fun calculateRoute(
        origin: LocationPoint,
        destination: LocationPoint,
        vehicle: Vehicle,
        initialBatteryPercent: Int,
        acActive: Boolean
    ): Result<RoutePlan> = withContext(Dispatchers.IO) {
        try {
            // RNF06: sin internet no se puede trazar una ruta nueva.
            // Se avisa enseguida (sin esperar timeouts) en vez de inventar un recorrido.
            if (!networkConnectivityObserver.isCurrentlyConnected) {
                return@withContext Result.failure(NoConnectionException())
            }

            val origLat = origin.latitude
            val origLon = origin.longitude
            val destLat = destination.latitude
            val destLon = destination.longitude

            // Camino real entre origen y destino (OSRM)
            val directRoute = fetchOsrmRoute("$origLon,$origLat;$destLon,$destLat")
            val directGeometry = directRoute?.geometry
            val directPoints = directGeometry?.let { RouteMapper.decodePolyline(it) }.orEmpty()
            if (directRoute == null || directGeometry == null || directPoints.isEmpty()) {
                return@withContext Result.failure(RouteServiceException())
            }

            var distanceKm = directRoute.distanceMeters / 1000.0
            var driveDurationMinutes = (directRoute.durationSeconds / 60.0).toInt()
            var polylinePoints: List<Pair<Double, Double>> = directPoints
            var encodedPolyline: String = directGeometry

            // RF02: estaciones compatibles con el auto cerca de la ruta (OpenChargeMap o Room)
            val stationsAlongRoute = stationRepository.getStationsAlongRoute(polylinePoints, vehicle.supportedConnectors)

            // Consumo: Base + penalidad del 10% si el aire acondicionado está encendido
            val acMultiplier = if (acActive) 1.10 else 1.00
            val consumptionKwhPer100Km = vehicle.baseConsumptionKwhPer100Km * acMultiplier

            // 1) Se planifican las paradas sobre la ruta directa (regla de negocio en el dominio)
            var chargingPlan = ChargingPlanner.plan(
                routeKm = distanceKm,
                stations = ChargingPlanner.locateOnRoute(stationsAlongRoute, polylinePoints, distanceKm),
                vehicle = vehicle,
                consumptionKwhPer100Km = consumptionKwhPer100Km,
                initialBatteryPercent = initialBatteryPercent
            )

            // Distancia del camino directo (para mostrar cuánto se desvía para cargar)
            val directDistanceKm = distanceKm

            // 2) Si hay paradas, la ruta tiene que pasar por los cargadores elegidos
            if (chargingPlan.stops.isNotEmpty()) {
                val chosenStations = chargingPlan.stops.map { it.station }
                val viaCoords = (listOf("$origLon,$origLat") +
                        chosenStations.map { "${it.longitude},${it.latitude}" } +
                        listOf("$destLon,$destLat")).joinToString(";")
                val viaRoute = fetchOsrmRoute(viaCoords)
                val viaGeometry = viaRoute?.geometry
                val viaPoints = viaGeometry?.let { RouteMapper.decodePolyline(it) }.orEmpty()
                if (viaRoute == null || viaGeometry == null || viaPoints.isEmpty()) {
                    return@withContext Result.failure(RouteServiceException())
                }

                distanceKm = viaRoute.distanceMeters / 1000.0
                driveDurationMinutes = (viaRoute.durationSeconds / 60.0).toInt()
                polylinePoints = viaPoints
                encodedPolyline = viaGeometry

                // 3) Se recalcula sobre la ruta nueva (que incluye los desvíos) con los cargadores elegidos
                chargingPlan = ChargingPlanner.plan(
                    routeKm = distanceKm,
                    stations = ChargingPlanner.locateOnRoute(chosenStations, polylinePoints, distanceKm),
                    vehicle = vehicle,
                    consumptionKwhPer100Km = consumptionKwhPer100Km,
                    initialBatteryPercent = initialBatteryPercent
                )
            }

            // Paradas con su horario estimado (manejo hasta ahí + cargas anteriores).
            // La salida se toma como el momento en que se calcula la ruta.
            val now = System.currentTimeMillis()
            fun clock(minutes: Int) = TripClock.at(now, minutes)
            var previousChargeMinutes = 0
            val stops = chargingPlan.stops.mapIndexed { index, planned ->
                val driveMinutes = if (distanceKm > 0) ((planned.km / distanceKm) * driveDurationMinutes).toInt() else 0
                val minutesToStop = driveMinutes + previousChargeMinutes
                previousChargeMinutes += planned.chargeMinutes
                TripStop(
                    id = "stop_${index + 1}_$now",
                    station = planned.station,
                    orderIndex = index + 1,
                    stopDurationMinutes = planned.chargeMinutes,
                    arrivalBatteryPercent = planned.arrivalBatteryPercent,
                    departureBatteryPercent = planned.departureBatteryPercent,
                    chargedEnergyKwh = planned.chargedEnergyKwh,
                    arrivalTime = clock(minutesToStop),
                    departureTime = clock(minutesToStop + planned.chargeMinutes)
                )
            }

            val chargeDurationMinutes = chargingPlan.stops.sumOf { it.chargeMinutes }
            val finalArrivalBatteryPercent = chargingPlan.arrivalBatteryPercent
            val totalDurationMinutes = driveDurationMinutes + chargeDurationMinutes

            val warning = when {
                !chargingPlan.reachesDestination ->
                    "Con las estaciones encontradas no alcanza la batería para llegar al destino. Revisá la carga inicial o el recorrido."
                finalArrivalBatteryPercent < 20 ->
                    "Batería estimada al arribar: $finalArrivalBatteryPercent%. Recomendamos mantener la carga programada."
                else -> null
            }

            val routePlan = RoutePlan(
                origin = origin,
                destination = destination,
                distanceKm = (distanceKm * 10.0).toInt() / 10.0,
                totalDurationMinutes = totalDurationMinutes,
                driveDurationMinutes = driveDurationMinutes,
                chargeDurationMinutes = chargeDurationMinutes,
                stops = stops,
                nearbyStations = stationsAlongRoute,
                polylinePoints = polylinePoints,
                encodedPolyline = encodedPolyline,
                arrivalBatteryPercent = finalArrivalBatteryPercent,
                consumptionKwhPer100Km = (consumptionKwhPer100Km * 10.0).toInt() / 10.0,
                calculatedAtMillis = now,
                directDistanceKm = directDistanceKm,
                departureTime = clock(0),
                arrivalTime = clock(totalDurationMinutes),
                isLowBatteryWarning = warning != null,
                warningMessage = warning
            )

            Result.success(routePlan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchOsrmRoute(coordinates: String): OsrmRoute? {
        return try {
            val response = osrmApi.getRoute(coordinates = coordinates)
            if (response.isSuccessful) response.body()?.routes?.firstOrNull() else null
        } catch (_: Exception) {
            null
        }
    }

    // Pide las indicaciones reales del camino. Sin internet devuelve la lista vacia.
    override suspend fun getNavigationSteps(plan: RoutePlan): List<NavStep> = withContext(Dispatchers.IO) {
        val coordinates = mutableListOf("${plan.origin.longitude},${plan.origin.latitude}")
        plan.stops.forEach { coordinates.add("${it.station.longitude},${it.station.latitude}") }
        coordinates.add("${plan.destination.longitude},${plan.destination.latitude}")

        val route = try {
            val response = osrmApi.getRoute(coordinates = coordinates.joinToString(";"), steps = true)
            if (response.isSuccessful) response.body()?.routes?.firstOrNull() else null
        } catch (_: Exception) {
            null
        }
        val legs = route?.legs
        if (route == null || legs == null || route.distanceMeters <= 0) {
            return@withContext emptyList()
        }

        val steps = mutableListOf<NavStep>()
        var walkedMeters = 0.0
        legs.forEachIndexed { legIndex, leg ->
            // El nombre del lugar al que se llega al final de este tramo
            val arrivalName = plan.stops.getOrNull(legIndex)?.station?.name ?: "tu destino"
            leg.steps?.forEach { step ->
                // La maniobra de "salir" no se muestra: la primera indicacion es la siguiente
                if (step.maneuver?.type != "depart") {
                    steps.add(
                        NavStep(
                            progress = (walkedMeters / route.distanceMeters).toFloat(),
                            instruction = buildInstruction(step, arrivalName),
                            maneuver = maneuverKey(step),
                            roadName = step.name ?: ""
                        )
                    )
                }
                walkedMeters += step.distanceMeters
            }
        }
        steps
    }

    private fun maneuverKey(step: OsrmStep): String {
        val type = step.maneuver?.type
        return when {
            type == "arrive" -> "arrive"
            type == "roundabout" || type == "rotary" -> "roundabout"
            else -> step.maneuver?.modifier ?: "straight"
        }
    }

    private fun buildInstruction(step: OsrmStep, arrivalName: String): String {
        val road = if (step.name.isNullOrBlank()) "" else " por ${step.name}"
        val direction = when (step.maneuver?.modifier) {
            "left" -> "a la izquierda"
            "right" -> "a la derecha"
            "slight left" -> "levemente a la izquierda"
            "slight right" -> "levemente a la derecha"
            "sharp left" -> "bien cerrado a la izquierda"
            "sharp right" -> "bien cerrado a la derecha"
            else -> ""
        }
        return when (step.maneuver?.type) {
            "arrive" -> "Llegás a $arrivalName"
            "roundabout", "rotary" -> {
                val exit = step.maneuver.exit
                if (exit != null) "En la rotonda, tomá la salida $exit$road" else "Entrá a la rotonda$road"
            }
            "fork" -> "Mantenete $direction$road".replace("  ", " ")
            "off ramp" -> "Tomá la salida $direction$road".replace("  ", " ")
            "on ramp" -> "Tomá el acceso $direction$road".replace("  ", " ")
            "merge" -> "Incorporate$road"
            "turn", "end of road" -> when {
                step.maneuver.modifier == "uturn" -> "Pegá la vuelta$road"
                direction.isEmpty() -> "Seguí derecho$road"
                else -> "Girá $direction$road"
            }
            else -> if (direction.isEmpty()) "Seguí derecho$road" else "Seguí $direction$road"
        }
    }

    // Convierte el texto escrito por el usuario en coordenadas.
    // Puede dar varias opciones, porque hay calles que existen en muchas ciudades.
    override suspend fun geocodeAddress(address: String): List<LocationPoint> = withContext(Dispatchers.IO) {
        val response = RetrofitClient.nominatimApi.search(address)
        val results = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        results
            // Una provincia o un pais no sirven como destino: su punto es el centro del territorio
            // (por ejemplo "Córdoba, Argentina" queda 70 km antes de la ciudad)
            .filter { it.addressType !in listOf("state", "region", "country") }
            .mapNotNull { result ->
            val lat = result.lat?.toDoubleOrNull()
            val lon = result.lon?.toDoubleOrNull()
            if (lat == null || lon == null) {
                null
            } else {
                LocationPoint(
                    name = address,
                    address = result.displayName ?: address,
                    latitude = lat,
                    longitude = lon
                )
            }
        }.distinctBy { it.address }
    }

    override fun getOfflineRoute(tripId: String): Flow<RoutePlan?> = flow {
        val routeEntity = database.routeDao().getRouteByTripId(tripId).firstOrNull()
        if (routeEntity == null) {
            emit(null)
            return@flow
        }
        val tripEntity = database.tripDao().getTripById(tripId).firstOrNull() ?: return@flow
        val stopsEntities = database.tripStopDao().getStopsByRouteIdSync(routeEntity.id)
        val stationEntities = database.stationDao().getStationsByIds(stopsEntities.map { it.stationId })
        val stationMap = stationEntities.associate { it.id to StationMapper.fromEntity(it) }

        val origin = LocationPoint(tripEntity.originName, tripEntity.originAddress, tripEntity.originLat, tripEntity.originLng)
        val dest = LocationPoint(tripEntity.destName, tripEntity.destAddress, tripEntity.destLat, tripEntity.destLng)

        emit(RouteMapper.fromEntities(origin, dest, routeEntity, stopsEntities, stationMap))
    }.flowOn(Dispatchers.IO)

    override suspend fun saveRoutePlan(tripId: String, plan: RoutePlan): Unit = withContext(Dispatchers.IO) {
        val routeEntity = RouteMapper.toEntity(tripId, plan)
        database.routeDao().insertOrUpdateRoute(routeEntity)

        plan.stops.forEach { stop ->
            database.stationDao().insertStation(StationMapper.toEntity(stop.station))
        }

        val stopEntities = RouteMapper.toStopEntities(routeEntity.id, plan.stops)
        database.tripStopDao().deleteStopsByRouteId(routeEntity.id)
        database.tripStopDao().insertStops(stopEntities)
        Unit
    }
}
