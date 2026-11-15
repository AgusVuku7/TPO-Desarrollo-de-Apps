package com.chargeroute.app

import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.usecase.UpdateBatteryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// Repositorio falso: solo guarda el ultimo porcentaje que le mandaron
private class RecordingTripRepository : TripRepository {
    var lastTripId: String? = null
    var lastPercent: Int? = null

    override suspend fun saveTrip(trip: Trip) {}

    override suspend fun updateTripBattery(tripId: String, newBatteryPercent: Int) {
        lastTripId = tripId
        lastPercent = newBatteryPercent
    }

    override suspend fun markTripCompleted(tripId: String) {}
    override fun getTripById(tripId: String): Flow<Trip?> = flowOf(null)
    override fun getSavedTrips(): Flow<List<Trip>> = flowOf(emptyList())
    override fun getLatestTrip(): Flow<Trip?> = flowOf(null)
}

class UpdateBatteryUseCaseTest {

    private val repository = RecordingTripRepository()
    private val useCase = UpdateBatteryUseCase(repository)

    @Test
    fun percentAbove100IsSavedAs100() = runTest {
        useCase("trip_1", 150)
        assertEquals(100, repository.lastPercent)
    }

    @Test
    fun negativePercentIsSavedAsZero() = runTest {
        useCase("trip_1", -5)
        assertEquals(0, repository.lastPercent)
    }

    @Test
    fun validPercentIsSavedAsIs() = runTest {
        useCase("trip_1", 55)
        assertEquals(55, repository.lastPercent)
    }

    @Test
    fun theLimitsThemselvesAreKept() = runTest {
        useCase("trip_1", 0)
        assertEquals(0, repository.lastPercent)
        useCase("trip_1", 100)
        assertEquals(100, repository.lastPercent)
    }

    @Test
    fun theTripIdIsPassedToTheRepository() = runTest {
        useCase("trip_42", 55)
        assertEquals("trip_42", repository.lastTripId)
    }
}
