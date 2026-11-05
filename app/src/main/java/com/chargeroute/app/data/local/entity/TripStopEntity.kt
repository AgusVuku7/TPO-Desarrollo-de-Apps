package com.chargeroute.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StationEntity::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["stationId"])
    ]
)
data class TripStopEntity(
    @PrimaryKey
    val id: String,
    val routeId: String,
    val stationId: String,
    val orderIndex: Int,
    val stopDurationMinutes: Int,
    val arrivalBatteryPercent: Int,
    val departureBatteryPercent: Int,
    val chargedEnergyKwh: Double,
    val arrivalTime: String,
    val departureTime: String
)
