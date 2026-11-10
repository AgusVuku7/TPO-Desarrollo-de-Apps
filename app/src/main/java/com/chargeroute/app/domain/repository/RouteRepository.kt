package com.chargeroute.app.domain.repository

import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.NavStep
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    suspend fun calculateRoute(
        origin: LocationPoint,
        destination: LocationPoint,
        vehicle: Vehicle,
        initialBatteryPercent: Int,
        acActive: Boolean
    ): Result<RoutePlan>

    fun getOfflineRoute(tripId: String): Flow<RoutePlan?>

    suspend fun geocodeAddress(address: String): List<LocationPoint>

    suspend fun getNavigationSteps(plan: RoutePlan): List<NavStep>
}
