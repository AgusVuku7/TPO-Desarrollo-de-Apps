package com.chargeroute.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chargeroute.app.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStations(stations: List<StationEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStation(station: StationEntity): Long

    @Query("SELECT * FROM stations WHERE id = :id")
    fun getStationById(id: String): StationEntity?

    @Query("SELECT * FROM stations")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id IN (:ids)")
    fun getStationsByIds(ids: List<String>): List<StationEntity>
}
