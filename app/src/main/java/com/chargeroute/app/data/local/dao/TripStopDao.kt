package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chargeroute.app.data.local.entity.TripStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripStopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStops(stops: List<TripStopEntity>): List<Long>

    @Query("SELECT * FROM trip_stops WHERE routeId = :routeId ORDER BY orderIndex ASC")
    fun getStopsByRouteId(routeId: String): Flow<List<TripStopEntity>>

    @Query("SELECT * FROM trip_stops WHERE routeId = :routeId ORDER BY orderIndex ASC")
    fun getStopsByRouteIdSync(routeId: String): List<TripStopEntity>

    @Query("DELETE FROM trip_stops WHERE routeId = :routeId")
    fun deleteStopsByRouteId(routeId: String): Int
}
