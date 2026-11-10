package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chargeroute.app.data.local.entity.TripStopEntity

@Dao
interface TripStopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<TripStopEntity>)

    @Query("SELECT * FROM trip_stops WHERE routeId = :routeId ORDER BY orderIndex ASC")
    suspend fun getStopsByRouteId(routeId: String): List<TripStopEntity>

    @Query("DELETE FROM trip_stops WHERE routeId = :routeId")
    suspend fun deleteStopsByRouteId(routeId: String)
}
