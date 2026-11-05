package com.chargeroute.app.domain.usecase

import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.RouteRepository

class CalculateRouteUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke(
        origin: LocationPoint,
        destination: LocationPoint,
        vehicle: Vehicle,
        initialBatteryPercent: Int,
        acActive: Boolean
    ): Result<RoutePlan> {
        return routeRepository.calculateRoute(
            origin = origin,
            destination = destination,
            vehicle = vehicle,
            initialBatteryPercent = initialBatteryPercent,
            acActive = acActive
        )
    }
}
