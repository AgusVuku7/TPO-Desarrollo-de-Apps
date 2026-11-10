package com.chargeroute.app.data.repository

import androidx.room.withTransaction
import com.chargeroute.app.data.local.ChargeRouteDatabase
import com.chargeroute.app.data.local.entity.TripEntity
import com.chargeroute.app.data.mapper.RouteMapper
import com.chargeroute.app.data.mapper.StationMapper
import com.chargeroute.app.data.mapper.TripMapper
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.repository.StationRepository
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class TripRepositoryImpl(
    private val database: ChargeRouteDatabase,
    private val vehicleRepository: VehicleRepository,
    private val stationRepository: StationRepository
) : TripRepository {

    // Viaje, ruta, estaciones y paradas se guardan juntos: si algo falla no queda un viaje a medias
    override suspend fun saveTrip(trip: Trip) {
        database.withTransaction {
            database.tripDao().insertOrUpdateTrip(TripMapper.toEntity(trip))

            val routeEntity = RouteMapper.toEntity(trip.id, trip.routePlan)
            database.routeDao().insertOrUpdateRoute(routeEntity)

            trip.routePlan.stops.forEach { stop ->
                database.stationDao().insertStation(StationMapper.toEntity(stop.station))
            }

            database.tripStopDao().deleteStopsByRouteId(routeEntity.id)
            database.tripStopDao().insertStops(RouteMapper.toStopEntities(routeEntity.id, trip.routePlan.stops))
        }
    }

    override suspend fun updateTripBattery(tripId: String, newBatteryPercent: Int) {
        database.tripDao().updateBattery(tripId, newBatteryPercent)
    }

    override suspend fun markTripCompleted(tripId: String) {
        database.tripDao().markCompleted(tripId)
    }

    override fun getTripById(tripId: String): Flow<Trip?> = flow {
        val tripEntity = database.tripDao().getTripById(tripId)
        emit(tripEntity?.let { toTrip(it, withNearbyStations = true) })
    }.flowOn(Dispatchers.IO)

    // Se actualiza sola cada vez que se guarda, termina o cambia la bateria de un viaje
    override fun getSavedTrips(): Flow<List<Trip>> =
        database.tripDao().observeAllTrips()
            .map { entities -> entities.mapNotNull { toTrip(it, withNearbyStations = false) } }
            .flowOn(Dispatchers.IO)

    override fun getLatestTrip(): Flow<Trip?> = flow {
        val tripEntity = database.tripDao().getLatestTrip()
        emit(tripEntity?.let { toTrip(it, withNearbyStations = true) })
    }.flowOn(Dispatchers.IO)

    /**
     * Arma el viaje completo a partir de lo guardado en Room (ruta, paradas y estaciones).
     * Devuelve null si el viaje no tiene ruta guardada.
     */
    private suspend fun toTrip(tripEntity: TripEntity, withNearbyStations: Boolean): Trip? {
        val routeEntity = database.routeDao().getRouteByTripId(tripEntity.id) ?: return null
        val vehicles = vehicleRepository.getAvailableVehicles()
        val vehicle = vehicles.firstOrNull { it.id == tripEntity.vehicleId } ?: vehicles.first()

        val stopEntities = database.tripStopDao().getStopsByRouteId(routeEntity.id)
        val stationMap = database.stationDao().getStationsByIds(stopEntities.map { it.stationId })
            .associate { it.id to StationMapper.fromEntity(it) }

        val origin = LocationPoint(tripEntity.originName, tripEntity.originAddress, tripEntity.originLat, tripEntity.originLng)
        val dest = LocationPoint(tripEntity.destName, tripEntity.destAddress, tripEntity.destLat, tripEntity.destLng)
        var plan = RouteMapper.fromEntities(origin, dest, routeEntity, stopEntities, stationMap)

        // Las estaciones cercanas no se guardan con la ruta: se buscan entre las guardadas en Room.
        // Asi el mapa del viaje muestra los cargadores aunque no haya conexion (RF04).
        if (withNearbyStations) {
            plan = plan.copy(
                nearbyStations = stationRepository.getCachedStationsAlongRoute(plan.polylinePoints, vehicle.supportedConnectors)
            )
        }
        return TripMapper.fromEntity(tripEntity, vehicle, plan)
    }
}
