package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chargeroute.app.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateRoute(route: RouteEntity): Long

    @Query("SELECT * FROM routes WHERE tripId = :tripId LIMIT 1")
    fun getRouteByTripId(tripId: String): Flow<RouteEntity?>

    @Query("DELETE FROM routes WHERE tripId = :tripId")
    fun deleteRouteByTripId(tripId: String): Int
}
