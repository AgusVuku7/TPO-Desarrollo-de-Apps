package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chargeroute.app.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    // @Upsert actualiza la fila si ya existe. REPLACE la borraba y la volvia a insertar,
    // y ese borrado eliminaba en cascada las paradas (trip_stops) de los viajes guardados.
    @Upsert
    fun insertStations(stations: List<StationEntity>): List<Long>

    @Upsert
    fun insertStation(station: StationEntity): Long

    @Query("SELECT * FROM stations WHERE id = :id")
    fun getStationById(id: String): StationEntity?

    @Query("SELECT * FROM stations")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations")
    fun getAllStationsList(): List<StationEntity>

    @Query("SELECT * FROM stations WHERE id IN (:ids)")
    fun getStationsByIds(ids: List<String>): List<StationEntity>
}
