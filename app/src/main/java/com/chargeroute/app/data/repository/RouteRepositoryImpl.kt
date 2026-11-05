package com.chargeroute.app.data.repository

import com.chargeroute.app.core.network.NetworkConnectivityObserver
import com.chargeroute.app.data.local.ChargeRouteDatabase
import com.chargeroute.app.data.mapper.RouteMapper
import com.chargeroute.app.data.mapper.StationMapper
import com.chargeroute.app.data.remote.RetrofitClient
import com.chargeroute.app.data.remote.dto.OsrmRoute
import com.chargeroute.app.data.remote.dto.OsrmStep
import com.chargeroute.app.domain.model.NavStep
import com.chargeroute.app.domain.model.RouteGeometry
import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RouteRepositoryImpl(
    private val database: ChargeRouteDatabase,
    private val networkConnectivityObserver: NetworkConnectivityObserver
) : RouteRepository {

    private val osrmApi = RetrofitClient.osrmApi

    // Red de estaciones de carga eléctrica verificadas en los principales corredores viales de Argentina
    private val knownStations = listOf(
        Station(
            id = "station_hudson_plaza",
            remoteId = "ocm_ar_hudson_plaza",
            name = "ChargeBox Net - Hudson Plaza Comercial",
            address = "Au. Bs As - La Plata km 33, Hudson",
            latitude = -34.7925,
            longitude = -58.1540,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 50.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("24 h", "Centro Comercial", "Café", "Baños"),
            operator = "ChargeBox Net",
            notes = "Cargador disponible en Hudson Plaza Comercial previa entrada a CABA.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_ypf_hudson",
            remoteId = "ocm_ar_ypf_hudson",
            name = "YPF Punto Eléctrico Hudson",
            address = "Autopista Buenos Aires - La Plata km 30, Hudson",
            latitude = -34.7890,
            longitude = -58.1580,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 100.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 3,
            totalCount = 4,
            amenities = listOf("24 h", "Full YPF", "Café / Restaurante", "Wi-Fi"),
            operator = "YPF Punto Eléctrico",
            notes = "Estación de carga ultra-rápida sobre Autopista Bs As - La Plata.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_dock_sud",
            remoteId = "ocm_ar_dock_sud",
            name = "Axion Energy Dock Sud (Entrada CABA)",
            address = "Autopista Buenos Aires - La Plata km 7, Dock Sud / Avellaneda",
            latitude = -34.6465,
            longitude = -58.3510,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 60.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("24 h", "Spot!", "Baños", "Wi-Fi"),
            operator = "Axion On",
            notes = "Estación estratégica antes del ingreso a CABA por Autopista Bs As - La Plata.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_uade_san_telmo",
            remoteId = "ocm_ar_uade",
            name = "ChargeBox Net - Zona UADE / Montserrat",
            address = "Lima 757 (Acceso UADE / Independencia), CABA",
            latitude = -34.6174,
            longitude = -58.3813,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 50.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("Estacionamiento", "UADE", "Seguridad 24h"),
            operator = "ChargeBox Net",
            notes = "Punto de carga rápida en zona UADE Montserrat.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_boedo_alcorta",
            remoteId = "ocm_ar_boedo",
            name = "YPF Punto Eléctrico Boedo / Alcorta",
            address = "Av. Amancio Alcorta y Av. Boedo / Jujuy, CABA",
            latitude = -34.6360,
            longitude = -58.4010,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 100.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 3,
            totalCount = 4,
            amenities = listOf("24 h", "Full YPF", "Baños"),
            operator = "YPF Punto Eléctrico",
            notes = "Punto de carga cerca del acceso a Autopista 25 de Mayo por Boedo / Jujuy.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_pinamar",
            remoteId = "ocm_ar_pinamar",
            name = "Estación YPF Punto Eléctrico Pinamar",
            address = "Av. Bunge y Av. Shaw, Pinamar",
            latitude = -37.1084,
            longitude = -56.8587,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 60.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("24 h", "Full YPF", "Wi-Fi"),
            operator = "YPF Punto Eléctrico",
            notes = "Carga en el acceso principal a Pinamar.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_crotto",
            remoteId = "ocm_ar_crotto",
            name = "YPF Esquina de Crotto (Ruta 11)",
            address = "Ruta 11 km 225, Esquina de Crotto / Conesa",
            latitude = -36.5240,
            longitude = -56.9850,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 100.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("24 h", "Restaurante", "Baños"),
            operator = "YPF Punto Eléctrico",
            notes = "Punto estratégico de carga sobre Ruta 11 empalme Pinamar.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_dolores",
            remoteId = "ocm_ar_210",
            name = "Punto de Carga YPF Dolores",
            address = "Área de servicio - Autovía 2, km 210, Dolores",
            latitude = -36.3132,
            longitude = -57.6792,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 120.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true),
                Connector(type = ConnectorType.CHADEMO, powerKw = 50.0, isCompatible = false)
            ),
            availableCount = 2,
            totalCount = 4,
            amenities = listOf("24 h", "Café / Restaurante", "Baños", "Wi-Fi"),
            operator = "YPF Punto Eléctrico",
            notes = "Carga ultra-rápida DC 120 kW en parador principal sobre Autovía 2.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_chascomus",
            remoteId = "ocm_ar_122",
            name = "Estación YPF Chascomús",
            address = "Autovía 2, km 122, Chascomús",
            latitude = -35.5786,
            longitude = -58.0125,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 60.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 1,
            totalCount = 2,
            amenities = listOf("24 h", "Minimercado", "Baños"),
            operator = "YPF Punto Eléctrico",
            notes = "Acceso directo sobre colectora.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_castelli",
            remoteId = "ocm_ar_142",
            name = "Área de Servicio Shell Castelli",
            address = "Autovía 2, km 142, Castelli",
            latitude = -36.0891,
            longitude = -57.8012,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 100.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 3,
            totalCount = 4,
            amenities = listOf("24 h", "Restaurante", "Baños", "Tienda"),
            operator = "Shell Recharge",
            notes = "Carga rápida disponible las 24 horas.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_samborombon",
            remoteId = "ocm_ar_090",
            name = "Parador Atalaya Samborombón",
            address = "Autovía 2, km 90, Chascomús",
            latitude = -35.2912,
            longitude = -58.1215,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 50.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("24 h", "Cafetería medialunas", "Baños"),
            operator = "ChargeBox Net",
            notes = "Parador clásico sobre Autovía 2 mano hacia la Costa.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_san_nicolas",
            remoteId = "ocm_ar_rn9",
            name = "YPF San Nicolás RN 9",
            address = "Ruta Nacional 9, km 230, San Nicolás",
            latitude = -33.3320,
            longitude = -60.2190,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 100.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("24 h", "Shop", "Wi-Fi"),
            operator = "YPF Punto Eléctrico",
            notes = "Punto estratégico corredor Buenos Aires - Rosario.",
            lastUpdatedMillis = System.currentTimeMillis()
        ),
        Station(
            id = "station_campana",
            remoteId = "ocm_ar_campana",
            name = "Axion Energy Campana",
            address = "Ruta Panamericana km 75, Campana",
            latitude = -34.1680,
            longitude = -58.9590,
            connectors = listOf(
                Connector(type = ConnectorType.CCS2, powerKw = 60.0, isCompatible = true),
                Connector(type = ConnectorType.TYPE_2, powerKw = 22.0, isCompatible = true)
            ),
            availableCount = 2,
            totalCount = 2,
            amenities = listOf("Spot!", "Baños", "Café"),
            operator = "Axion On",
            notes = "Acceso por autopista.",
            lastUpdatedMillis = System.currentTimeMillis()
        )
    )

    override suspend fun calculateRoute(
        origin: LocationPoint,
        destination: LocationPoint,
        vehicle: Vehicle,
        initialBatteryPercent: Int,
        acActive: Boolean
    ): Result<RoutePlan> = withContext(Dispatchers.IO) {
        try {
            val isOnline = networkConnectivityObserver.isCurrentlyConnected

            // Coordenadas seguras y validadas
            val origLat = origin.latitude
            val origLon = origin.longitude
            val destLat = destination.latitude
            val destLon = destination.longitude

            // Distancia geodésica base
            val straightLineKm = calculateHaversineDistanceKm(origLat, origLon, destLat, destLon)

            // Detección de ruta conocida Buenos Aires - Mar del Plata (Autovía 2 ~404 km)
            val isBaToMdp = isCorridor(origLat, origLon, destLat, destLon, -34.6, -58.38, -38.0, -57.54)

            var distanceKm: Double = when {
                isBaToMdp -> 404.0
                straightLineKm in 40.0..80.0 -> straightLineKm * 1.15
                else -> straightLineKm * 1.18 // Factor de tortuosidad vial típico argentino
            }

            var driveDurationMinutes = (distanceKm / 85.0 * 60.0).toInt().coerceAtLeast(30)
            var polylinePoints: List<Pair<Double, Double>> = generateInterpolatedRoute(origin, destination)
            // Se guarda codificada de verdad: si no, al leerla de Room salian puntos fuera de la ruta
            var encodedPolyline = RouteMapper.encodePolyline(polylinePoints)
            var isApproximate = true // pasa a false si el servidor devuelve la ruta real

            if (isOnline) {
                try {
                    val coords = "$origLon,$origLat;$destLon,$destLat"
                    val response = osrmApi.getRoute(coordinates = coords)
                    if (response.isSuccessful) {
                        val route = response.body()?.routes?.firstOrNull()
                        if (route != null && route.distanceMeters > 1000) {
                            val osrmDistKm = route.distanceMeters / 1000.0
                            // Si la distancia calculada por OSRM es coherente con la línea recta (< 2.5x)
                            // En viajes cortos la ruta real puede dar mucha mas vuelta, asi que se acepta igual
                            if (osrmDistKm < straightLineKm * 2.5 || straightLineKm < 30.0) {
                                distanceKm = osrmDistKm
                                driveDurationMinutes = (route.durationSeconds / 60.0).toInt()
                                route.geometry?.let { geom ->
                                    encodedPolyline = geom
                                    val decoded = RouteMapper.decodePolyline(geom)
                                    if (decoded.isNotEmpty()) {
                                        polylinePoints = decoded
                                        isApproximate = false
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fallback transparente al modelo offline
                }
            }

            // Consumo: Base + penalidad del 10% si el aire acondicionado está encendido
            val acMultiplier = if (acActive) 1.10 else 1.00
            val consumptionKwhPer100Km = vehicle.baseConsumptionKwhPer100Km * acMultiplier
            val totalEnergyNeededKwh = (distanceKm * consumptionKwhPer100Km) / 100.0

            val usableCapacity = vehicle.usableBatteryKwh
            val availableInitialEnergyKwh = (initialBatteryPercent / 100.0) * usableCapacity

            val stops = mutableListOf<TripStop>()
            var chargeDurationMinutes = 0
            var finalArrivalBatteryPercent: Int

            // Reserva de seguridad mínima requerida al llegar: 15%
            val safetyReserveKwh = usableCapacity * 0.15

            // Requiere parada si la energía no alcanza para la ruta + reserva, o si la batería inicial es <= 30% y la llegada quedaría baja (< 25%)
            val needsCharge = availableInitialEnergyKwh < totalEnergyNeededKwh + safetyReserveKwh ||
                    (initialBatteryPercent <= 30 && (availableInitialEnergyKwh - totalEnergyNeededKwh) < usableCapacity * 0.25)

            if (needsCharge) {
                // Requiere al menos una parada intermedia de recarga
                // Buscar una estación cerca de la ruta a la que se pueda llegar con la batería actual
                val reachableKm = (availableInitialEnergyKwh / consumptionKwhPer100Km) * 100.0
                val candidateStation = findBestStationAlongRoute(polylinePoints, vehicle, distanceKm, reachableKm)

                if (candidateStation == null) {
                    // No hay cargadores cerca de esta ruta: se informa la batería real, sin parada
                    val energyRemaining = availableInitialEnergyKwh - totalEnergyNeededKwh
                    finalArrivalBatteryPercent = ((energyRemaining / usableCapacity) * 100.0).toInt().coerceIn(0, 100)
                } else {
                    // Se recalcula la ruta para que pase por el cargador y la parada quede sobre la línea
                    val viaCoords = "$origLon,$origLat;${candidateStation.longitude},${candidateStation.latitude};$destLon,$destLat"
                    val viaRoute = if (isOnline) fetchOsrmRoute(viaCoords) else null
                    val viaGeometry = viaRoute?.geometry
                    val viaPoints = if (viaGeometry != null) RouteMapper.decodePolyline(viaGeometry) else emptyList()

                    val distToStation: Double
                    if (viaRoute != null && viaGeometry != null && viaPoints.isNotEmpty()) {
                        distanceKm = viaRoute.distanceMeters / 1000.0
                        driveDurationMinutes = (viaRoute.durationSeconds / 60.0).toInt()
                        polylinePoints = viaPoints
                        encodedPolyline = viaGeometry
                        isApproximate = false
                        distToStation = viaRoute.legs?.firstOrNull()?.distanceMeters?.div(1000.0)
                            ?: (RouteGeometry.nearestOnRoute(viaPoints, candidateStation.latitude, candidateStation.longitude).progress * distanceKm)
                    } else {
                        // Sin internet: línea origen -> cargador -> destino
                        val stationPoint = LocationPoint(candidateStation.name, candidateStation.address, candidateStation.latitude, candidateStation.longitude)
                        polylinePoints = generateInterpolatedRoute(origin, stationPoint) + generateInterpolatedRoute(stationPoint, destination)
                        encodedPolyline = RouteMapper.encodePolyline(polylinePoints)
                        isApproximate = true
                        distToStation = RouteGeometry.nearestOnRoute(polylinePoints, candidateStation.latitude, candidateStation.longitude).progress * distanceKm
                    }

                    val energyToStation = (distToStation * consumptionKwhPer100Km) / 100.0
                    val arrivalBatteryAtStation = (((availableInitialEnergyKwh - energyToStation) / usableCapacity) * 100.0)
                        .toInt()
                        .coerceIn(0, 100)

                    // Tramo desde la estación hasta el destino
                    val distFromStationToDest = distanceKm - distToStation
                    val energyFromStation = (distFromStationToDest * consumptionKwhPer100Km) / 100.0

                    // Cargar por lo menos a 75%, o lo que haga falta para llegar con reserva (nunca más de 100%)
                    val neededPercent = (((energyFromStation + safetyReserveKwh) / usableCapacity) * 100.0).toInt()
                    val departureBatteryAtStation = neededPercent.coerceIn(maxOf(75, arrivalBatteryAtStation), 100)
                    val energyToChargeKwh = ((departureBatteryAtStation - arrivalBatteryAtStation) / 100.0) * usableCapacity
                    // Tiempo de carga estimado según potencia máxima de estación y vehículo
                    val maxKw = minOf(candidateStation.connectors.maxOfOrNull { it.powerKw } ?: 50.0, vehicle.maxDcKw)
                    val chargeTimeMin = ((energyToChargeKwh / maxKw) * 60.0).toInt().coerceIn(5, 240)
                    chargeDurationMinutes += chargeTimeMin

                    val minutesToStation = if (distanceKm > 0) ((distToStation / distanceKm) * driveDurationMinutes).toInt() else 0
                    val stop = TripStop(
                        id = "stop_1_${System.currentTimeMillis()}",
                        station = candidateStation,
                        orderIndex = 1,
                        stopDurationMinutes = chargeTimeMin,
                        arrivalBatteryPercent = arrivalBatteryAtStation,
                        departureBatteryPercent = departureBatteryAtStation,
                        chargedEnergyKwh = (energyToChargeKwh * 10.0).toInt() / 10.0,
                        arrivalTime = calculateArrivalTime(minutesToStation),
                        departureTime = calculateArrivalTime(minutesToStation + chargeTimeMin)
                    )
                    stops.add(stop)

                    val energyAtDest = ((departureBatteryAtStation / 100.0) * usableCapacity) - energyFromStation
                    finalArrivalBatteryPercent = ((energyAtDest / usableCapacity) * 100.0).toInt().coerceIn(0, 100)
                }
            } else {
                // El viaje se puede realizar directo sin paradas intermedias obligatorias
                val energyRemaining = availableInitialEnergyKwh - totalEnergyNeededKwh
                finalArrivalBatteryPercent = ((energyRemaining / usableCapacity) * 100.0).toInt().coerceIn(5, 100)
            }

            // Persistir estaciones en base de datos Room
            database.stationDao().insertStations(knownStations.map { StationMapper.toEntity(it) })

            val totalDurationMinutes = driveDurationMinutes + chargeDurationMinutes

            val routePlan = RoutePlan(
                origin = origin,
                destination = destination,
                distanceKm = (distanceKm * 10.0).toInt() / 10.0,
                totalDurationMinutes = totalDurationMinutes,
                driveDurationMinutes = driveDurationMinutes,
                chargeDurationMinutes = chargeDurationMinutes,
                stops = stops,
                polylinePoints = polylinePoints,
                encodedPolyline = encodedPolyline,
                isApproximate = isApproximate,
                arrivalBatteryPercent = finalArrivalBatteryPercent,
                consumptionKwhPer100Km = (consumptionKwhPer100Km * 10.0).toInt() / 10.0,
                departureTime = "09:41",
                arrivalTime = calculateArrivalTime(totalDurationMinutes),
                isLowBatteryWarning = finalArrivalBatteryPercent < 20,
                warningMessage = if (finalArrivalBatteryPercent < 20) {
                    "Batería estimada al arribar: $finalArrivalBatteryPercent%. Recomendamos mantener la carga programada."
                } else null
            )

            Result.success(routePlan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isCorridor(lat1: Double, lon1: Double, lat2: Double, lon2: Double, tLat1: Double, tLon1: Double, tLat2: Double, tLon2: Double): Boolean {
        val d1 = calculateHaversineDistanceKm(lat1, lon1, tLat1, tLon1)
        val d2 = calculateHaversineDistanceKm(lat2, lon2, tLat2, tLon2)
        val dRev1 = calculateHaversineDistanceKm(lat1, lon1, tLat2, tLon2)
        val dRev2 = calculateHaversineDistanceKm(lat2, lon2, tLat1, tLon1)
        return (d1 < 70.0 && d2 < 70.0) || (dRev1 < 70.0 && dRev2 < 70.0)
    }

    private fun findBestStationAlongRoute(
        polyline: List<Pair<Double, Double>>,
        vehicle: Vehicle,
        routeKm: Double,
        reachableKm: Double
    ): Station? {
        val compatible = knownStations.filter { it.isCompatibleWith(vehicle.supportedConnectors) }
        val pool = compatible.ifEmpty { knownStations }

        // Solo sirven las estaciones que quedan a menos de 30 km de la ruta
        val nearStations = pool.filter { station ->
            RouteGeometry.nearestOnRoute(polyline, station.latitude, station.longitude).distanceToRouteKm <= 30.0
        }
        if (nearStations.isEmpty()) return null

        // De las que se llega con la batería actual, la más avanzada en el viaje.
        // Si no se llega a ninguna, la primera del camino.
        val reachable = nearStations.filter { station ->
            RouteGeometry.nearestOnRoute(polyline, station.latitude, station.longitude).progress * routeKm <= reachableKm
        }
        return reachable.maxByOrNull { RouteGeometry.nearestOnRoute(polyline, it.latitude, it.longitude).progress }
            ?: nearStations.minByOrNull { RouteGeometry.nearestOnRoute(polyline, it.latitude, it.longitude).progress }
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
        val roadLower = step.name?.lowercase() ?: ""
        if (roadLower.contains("25 de mayo") || roadLower.contains("jujuy") || roadLower.contains("boedo")) {
            return "Subida a Autopista 25 de Mayo (disponible por Av. Boedo o Av. Jujuy)$road"
        }
        return when (step.maneuver?.type) {
            "arrive" -> "Llegás a $arrivalName"
            "roundabout", "rotary" -> {
                val exit = step.maneuver.exit
                if (exit != null) "En la rotonda, tomá la salida $exit$road" else "Entrá a la rotonda$road"
            }
            "fork" -> "Mantenete $direction$road".replace("  ", " ")
            "off ramp" -> "Tomá la salida $direction$road".replace("  ", " ")
            "on ramp" -> "Tomá el acceso $direction$road (opciones por Av. Boedo / Av. Jujuy)".replace("  ", " ")
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
        results.mapNotNull { result ->
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

    private fun calculateArrivalTime(durationMinutes: Int): String {
        val startHour = 9
        val startMin = 41
        val totalMin = startHour * 60 + startMin + durationMinutes
        val hour = (totalMin / 60) % 24
        val min = totalMin % 60
        return String.format("%02d:%02d", hour, min)
    }

    override suspend fun getStationsAlongRoute(
        polyline: List<Pair<Double, Double>>,
        vehicle: Vehicle
    ): List<Station> = withContext(Dispatchers.IO) {
        val compatible = knownStations.filter { it.isCompatibleWith(vehicle.supportedConnectors) }
        compatible.ifEmpty { knownStations }
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

    private fun generateInterpolatedRoute(
        origin: LocationPoint,
        dest: LocationPoint
    ): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val steps = 30
        for (i in 0..steps) {
            val fraction = i.toDouble() / steps.toDouble()
            val lat = origin.latitude + (dest.latitude - origin.latitude) * fraction
            val lng = origin.longitude + (dest.longitude - origin.longitude) * fraction
            points.add(Pair(lat, lng))
        }
        return points
    }

    private fun calculateHaversineDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
