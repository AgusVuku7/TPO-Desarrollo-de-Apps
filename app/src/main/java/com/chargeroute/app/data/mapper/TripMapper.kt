package com.chargeroute.app.data.mapper

import com.chargeroute.app.data.local.entity.TripEntity
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.Vehicle

object TripMapper {

    fun toEntity(trip: Trip): TripEntity {
        return TripEntity(
            id = trip.id,
            originName = trip.origin.name,
            originAddress = trip.origin.address,
            originLat = trip.origin.latitude,
            originLng = trip.origin.longitude,
            destName = trip.destination.name,
            destAddress = trip.destination.address,
            destLat = trip.destination.latitude,
            destLng = trip.destination.longitude,
            vehicleId = trip.vehicle.id,
            vehicleDisplayName = trip.vehicle.displayName,
            initialBatteryPercent = trip.initialBatteryPercent,
            currentBatteryPercent = trip.currentBatteryPercent,
            vehicleRangeKm = trip.vehicle.wltpRangeKm,
            vehicleConnector = trip.vehicle.supportedConnectors.firstOrNull()?.name ?: "CCS2",
            acActive = trip.acActive,
            isCompleted = trip.isCompleted,
            createdAtMillis = trip.createdAtMillis
        )
    }

    fun fromEntity(entity: TripEntity, vehicle: Vehicle, routePlan: RoutePlan): Trip {
        return Trip(
            id = entity.id,
            origin = LocationPoint(
                name = entity.originName,
                address = entity.originAddress,
                latitude = entity.originLat,
                longitude = entity.originLng
            ),
            destination = LocationPoint(
                name = entity.destName,
                address = entity.destAddress,
                latitude = entity.destLat,
                longitude = entity.destLng
            ),
            vehicle = vehicle,
            initialBatteryPercent = entity.initialBatteryPercent,
            currentBatteryPercent = entity.currentBatteryPercent,
            acActive = entity.acActive,
            routePlan = routePlan,
            createdAtMillis = entity.createdAtMillis,
            isCompleted = entity.isCompleted
        )
    }
}
