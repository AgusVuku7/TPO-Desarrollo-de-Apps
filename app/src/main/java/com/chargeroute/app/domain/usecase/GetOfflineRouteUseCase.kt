package com.chargeroute.app.domain.usecase

import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow

/**
 * RF04 & RNF01 - Guardar y consultar ruta Offline.
 */
class GetOfflineRouteUseCase(
    private val routeRepository: RouteRepository
) {
    operator fun invoke(tripId: String): Flow<RoutePlan?> {
        return routeRepository.getOfflineRoute(tripId)
    }
}
