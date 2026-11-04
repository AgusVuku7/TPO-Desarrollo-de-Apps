package com.chargeroute.app.domain.usecase

import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.repository.RouteRepository

class GeocodeAddressUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke(address: String): List<LocationPoint> {
        return routeRepository.geocodeAddress(address)
    }
}
