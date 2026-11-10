package com.chargeroute.app.domain.repository

import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.NavStep
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Vehicle

interface RouteRepository {
    suspend fun calculateRoute(
        origin: LocationPoint,
        destination: LocationPoint,
        vehicle: Vehicle,
        initialBatteryPercent: Int,
        acActive: Boolean
    ): Result<RoutePlan>

    suspend fun geocodeAddress(address: String): List<LocationPoint>

    suspend fun getNavigationSteps(plan: RoutePlan): List<NavStep>
}
