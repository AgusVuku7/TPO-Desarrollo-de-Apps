package com.chargeroute.app.domain.usecase

import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow

class GetVehicleCatalogUseCase(
    private val vehicleRepository: VehicleRepository
) {
    fun getAvailableVehicles(): List<Vehicle> {
        return vehicleRepository.getAvailableVehicles()
    }

    fun getDefaultVehicle(): Flow<Vehicle> {
        return vehicleRepository.getDefaultVehicle()
    }

    suspend fun saveSelectedVehicle(vehicle: Vehicle) {
        vehicleRepository.saveSelectedVehicle(vehicle)
    }
}
