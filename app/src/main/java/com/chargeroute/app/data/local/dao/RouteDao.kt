package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chargeroute.app.data.local.entity.RouteEntity

@Dao
interface RouteDao {
    // @Upsert en vez de REPLACE: no borra la ruta, asi no se pierden en cascada sus paradas
    @Upsert
    suspend fun insertOrUpdateRoute(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE tripId = :tripId LIMIT 1")
    suspend fun getRouteByTripId(tripId: String): RouteEntity?
}
