package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.chargeroute.app.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    // @Upsert en vez de REPLACE: no borra el viaje, asi no se pierden en cascada su ruta y paradas
    @Upsert
    fun insertOrUpdateTrip(trip: TripEntity): Long

    @Update
    fun updateTrip(trip: TripEntity): Int

    @Query("UPDATE trips SET currentBatteryPercent = :newBatteryPercent WHERE id = :tripId")
    fun updateBattery(tripId: String, newBatteryPercent: Int): Int

    @Query("UPDATE trips SET isCompleted = 1 WHERE id = :tripId")
    fun markCompleted(tripId: String): Int

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripById(tripId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips ORDER BY createdAtMillis DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY createdAtMillis DESC LIMIT 1")
    fun getLatestTrip(): Flow<TripEntity?>

    @Query("DELETE FROM trips WHERE id = :tripId")
    fun deleteTrip(tripId: String): Int
}
