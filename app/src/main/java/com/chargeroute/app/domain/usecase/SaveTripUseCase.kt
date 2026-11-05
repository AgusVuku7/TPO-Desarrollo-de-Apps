package com.chargeroute.app.domain.usecase

import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.repository.TripRepository

class SaveTripUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(trip: Trip) {
        tripRepository.saveTrip(trip)
    }
}
