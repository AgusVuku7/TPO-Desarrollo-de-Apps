package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chargeroute.app.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    // @Upsert en vez de REPLACE: no borra el viaje, asi no se pierden en cascada su ruta y paradas
    @Upsert
    suspend fun insertOrUpdateTrip(trip: TripEntity)

    @Query("UPDATE trips SET currentBatteryPercent = :newBatteryPercent WHERE id = :tripId")
    suspend fun updateBattery(tripId: String, newBatteryPercent: Int)

    @Query("UPDATE trips SET isCompleted = 1 WHERE id = :tripId")
    suspend fun markCompleted(tripId: String)

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: String): TripEntity?

    // Flow: avisa solo cada vez que cambia la tabla de viajes
    @Query("SELECT * FROM trips ORDER BY createdAtMillis DESC")
    fun observeAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun getLatestTrip(): TripEntity?
}
