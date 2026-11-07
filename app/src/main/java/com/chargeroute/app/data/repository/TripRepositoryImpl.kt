package com.chargeroute.app.data.repository

import com.chargeroute.app.data.local.ChargeRouteDatabase
import com.chargeroute.app.data.mapper.RouteMapper
import com.chargeroute.app.data.mapper.StationMapper
import com.chargeroute.app.data.mapper.TripMapper
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.StationRepository
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TripRepositoryImpl(
    private val database: ChargeRouteDatabase,
    private val vehicleRepository: VehicleRepository,
    private val stationRepository: StationRepository
) : TripRepository {

    // Las estaciones cercanas no se guardan con la ruta: se buscan entre las guardadas en Room.
    // Asi el mapa del viaje muestra los cargadores aunque no haya conexion (RF04).
    private suspend fun withNearbyStations(plan: RoutePlan, vehicle: Vehicle): RoutePlan {
        val stations = stationRepository.getCachedStationsAlongRoute(plan.polylinePoints, vehicle.supportedConnectors)
        return plan.copy(nearbyStations = stations)
    }

    override suspend fun saveTrip(trip: Trip): Unit = withContext(Dispatchers.IO) {
        val tripEntity = TripMapper.toEntity(trip)
        database.tripDao().insertOrUpdateTrip(tripEntity)

        // Save route
        val routeEntity = RouteMapper.toEntity(trip.id, trip.routePlan)
        database.routeDao().insertOrUpdateRoute(routeEntity)

        // Save stations
        trip.routePlan.stops.forEach { stop ->
            database.stationDao().insertStation(StationMapper.toEntity(stop.station))
        }

        // Save stops
        val stopEntities = RouteMapper.toStopEntities(routeEntity.id, trip.routePlan.stops)
        database.tripStopDao().deleteStopsByRouteId(routeEntity.id)
        database.tripStopDao().insertStops(stopEntities)
        Unit
    }

    override suspend fun updateTripBattery(tripId: String, newBatteryPercent: Int): Unit = withContext(Dispatchers.IO) {
        database.tripDao().updateBattery(tripId, newBatteryPercent)
        Unit
    }

    override suspend fun markTripCompleted(tripId: String): Unit = withContext(Dispatchers.IO) {
        database.tripDao().markCompleted(tripId)
        Unit
    }

    override fun getTripById(tripId: String): Flow<Trip?> = flow {
        val tripEntity = database.tripDao().getTripById(tripId).firstOrNull()
        if (tripEntity == null) {
            emit(null)
            return@flow
        }
        val vehicle = vehicleRepository.getAvailableVehicles().firstOrNull { it.id == tripEntity.vehicleId }
            ?: vehicleRepository.getAvailableVehicles().first()

        val routeEntity = database.routeDao().getRouteByTripId(tripId).firstOrNull()
        val origin = LocationPoint(tripEntity.originName, tripEntity.originAddress, tripEntity.originLat, tripEntity.originLng)
        val dest = LocationPoint(tripEntity.destName, tripEntity.destAddress, tripEntity.destLat, tripEntity.destLng)

        val plan = if (routeEntity != null) {
            val stopEntities = database.tripStopDao().getStopsByRouteIdSync(routeEntity.id)
            val stationEntities = database.stationDao().getStationsByIds(stopEntities.map { it.stationId })
            val stationMap = stationEntities.associate { it.id to StationMapper.fromEntity(it) }
            RouteMapper.fromEntities(origin, dest, routeEntity, stopEntities, stationMap)
        } else {
            // Default placeholder
            null
        }

        if (plan != null) {
            emit(TripMapper.fromEntity(tripEntity, vehicle, withNearbyStations(plan, vehicle)))
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    override fun getSavedTrips(): Flow<List<Trip>> = flow {
        val entities = database.tripDao().getAllTrips().firstOrNull() ?: emptyList()
        val vehicles = vehicleRepository.getAvailableVehicles()

        val trips = entities.mapNotNull { tripEntity ->
            val vehicle = vehicles.firstOrNull { it.id == tripEntity.vehicleId } ?: vehicles.first()
            val routeEntity = database.routeDao().getRouteByTripId(tripEntity.id).firstOrNull() ?: return@mapNotNull null
            val stopEntities = database.tripStopDao().getStopsByRouteIdSync(routeEntity.id)
            val stationEntities = database.stationDao().getStationsByIds(stopEntities.map { it.stationId })
            val stationMap = stationEntities.associate { it.id to StationMapper.fromEntity(it) }

            val origin = LocationPoint(tripEntity.originName, tripEntity.originAddress, tripEntity.originLat, tripEntity.originLng)
            val dest = LocationPoint(tripEntity.destName, tripEntity.destAddress, tripEntity.destLat, tripEntity.destLng)
            val plan = RouteMapper.fromEntities(origin, dest, routeEntity, stopEntities, stationMap)
            TripMapper.fromEntity(tripEntity, vehicle, plan)
        }
        emit(trips)
    }.flowOn(Dispatchers.IO)

    override fun getLatestTrip(): Flow<Trip?> = flow {
        val tripEntity = database.tripDao().getLatestTrip().firstOrNull()
        if (tripEntity == null) {
            emit(null)
            return@flow
        }
        val vehicle = vehicleRepository.getAvailableVehicles().firstOrNull { it.id == tripEntity.vehicleId }
            ?: vehicleRepository.getAvailableVehicles().first()
        val routeEntity = database.routeDao().getRouteByTripId(tripEntity.id).firstOrNull()
        if (routeEntity == null) {
            emit(null)
            return@flow
        }
        val stopEntities = database.tripStopDao().getStopsByRouteIdSync(routeEntity.id)
        val stationEntities = database.stationDao().getStationsByIds(stopEntities.map { it.stationId })
        val stationMap = stationEntities.associate { it.id to StationMapper.fromEntity(it) }

        val origin = LocationPoint(tripEntity.originName, tripEntity.originAddress, tripEntity.originLat, tripEntity.originLng)
        val dest = LocationPoint(tripEntity.destName, tripEntity.destAddress, tripEntity.destLat, tripEntity.destLng)
        val plan = RouteMapper.fromEntities(origin, dest, routeEntity, stopEntities, stationMap)

        emit(TripMapper.fromEntity(tripEntity, vehicle, withNearbyStations(plan, vehicle)))
    }.flowOn(Dispatchers.IO)
}
