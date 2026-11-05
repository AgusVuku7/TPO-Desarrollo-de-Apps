package com.chargeroute.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String,
    val originName: String,
    val originAddress: String,
    val originLat: Double,
    val originLng: Double,
    val destName: String,
    val destAddress: String,
    val destLat: Double,
    val destLng: Double,
    val vehicleId: String,
    val vehicleDisplayName: String,
    val initialBatteryPercent: Int,
    val currentBatteryPercent: Int,
    val vehicleRangeKm: Double,
    val vehicleConnector: String,
    val acActive: Boolean,
    val isCompleted: Boolean,
    val createdAtMillis: Long
)
