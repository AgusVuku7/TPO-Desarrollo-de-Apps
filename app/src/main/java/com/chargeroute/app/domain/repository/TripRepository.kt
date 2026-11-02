package com.chargeroute.app.domain.repository

import com.chargeroute.app.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    suspend fun saveTrip(trip: Trip)
    suspend fun updateTripBattery(tripId: String, newBatteryPercent: Int)
    suspend fun markTripCompleted(tripId: String)
    fun getTripById(tripId: String): Flow<Trip?>
    fun getSavedTrips(): Flow<List<Trip>>
    fun getLatestTrip(): Flow<Trip?>
}
