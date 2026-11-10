package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chargeroute.app.data.local.entity.StationEntity

@Dao
interface StationDao {
    // @Upsert actualiza la fila si ya existe. REPLACE la borraba y la volvia a insertar,
    // y ese borrado eliminaba en cascada las paradas (trip_stops) de los viajes guardados.
    @Upsert
    suspend fun insertStations(stations: List<StationEntity>)

    @Upsert
    suspend fun insertStation(station: StationEntity)

    @Query("SELECT * FROM stations")
    suspend fun getAllStations(): List<StationEntity>

    @Query("SELECT * FROM stations WHERE id IN (:ids)")
    suspend fun getStationsByIds(ids: List<String>): List<StationEntity>
}
