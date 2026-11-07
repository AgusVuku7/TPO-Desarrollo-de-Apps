package com.chargeroute.app.data.local.entity

import androidx.room.ColumnInfo
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
    val calculatedAtMillis: Long,
    // Distancia del camino directo, sin pasar por los cargadores (agregada en la version 2 de la base)
    @ColumnInfo(defaultValue = "0")
    val directDistanceKm: Double = 0.0
)
