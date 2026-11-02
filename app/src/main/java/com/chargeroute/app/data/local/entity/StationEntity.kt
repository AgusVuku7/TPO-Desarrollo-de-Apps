package com.chargeroute.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey
    val id: String,
    val remoteId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val connectorsJson: String,
    val availableCount: Int,
    val totalCount: Int,
    val amenitiesJson: String,
    val operator: String,
    val notes: String,
    val lastUpdatedMillis: Long
)
