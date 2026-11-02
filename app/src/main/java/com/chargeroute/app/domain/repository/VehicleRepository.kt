package com.chargeroute.app.domain.repository

import com.chargeroute.app.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun getAvailableVehicles(): List<Vehicle>
    fun getDefaultVehicle(): Flow<Vehicle>
    suspend fun saveSelectedVehicle(vehicle: Vehicle)
}
