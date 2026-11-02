package com.chargeroute.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"], unique = true)]
)
data class RouteEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val encodedPolyline: String,
    val distanceKm: Double,
    val totalDurationMinutes: Int,
    val driveDurationMinutes: Int,
    val chargeDurationMinutes: Int,
    val arrivalBatteryPercent: Int,
    val consumptionKwhPer100Km: Double,
    val calculatedAtMillis: Long
)
