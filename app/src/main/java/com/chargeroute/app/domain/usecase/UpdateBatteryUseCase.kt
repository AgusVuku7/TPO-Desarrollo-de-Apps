package com.chargeroute.app.domain.usecase

import com.chargeroute.app.domain.repository.TripRepository

/**
 * RF03 - Actualizar nivel de batería en ruta.
 * Recalcula la autonomía restante y la viabilidad del trayecto.
 */
class UpdateBatteryUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String, newBatteryPercent: Int) {
        val clampedPercent = newBatteryPercent.coerceIn(0, 100)
        tripRepository.updateTripBattery(tripId, clampedPercent)
    }
}
