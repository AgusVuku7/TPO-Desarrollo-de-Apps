package com.chargeroute.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OsrmRouteResponse(
    @SerializedName("code") val code: String?,
    @SerializedName("routes") val routes: List<OsrmRoute>?,
    @SerializedName("waypoints") val waypoints: List<OsrmWaypoint>?
)

data class OsrmRoute(
    @SerializedName("distance") val distanceMeters: Double,
    @SerializedName("duration") val durationSeconds: Double,
    @SerializedName("geometry") val geometry: String?,
    @SerializedName("weight") val weight: Double?,
    @SerializedName("legs") val legs: List<OsrmLeg>? = null
)

// Cada tramo entre dos puntos de la ruta (ej: origen -> cargador)
data class OsrmLeg(
    @SerializedName("distance") val distanceMeters: Double,
    @SerializedName("steps") val steps: List<OsrmStep>? = null
)

// Cada indicacion del camino (girar, seguir, rotonda...)
data class OsrmStep(
    @SerializedName("distance") val distanceMeters: Double,
    @SerializedName("name") val name: String?,
    @SerializedName("maneuver") val maneuver: OsrmManeuver?
)

data class OsrmManeuver(
    @SerializedName("type") val type: String?,
    @SerializedName("modifier") val modifier: String?,
    @SerializedName("exit") val exit: Int?
)

data class OsrmWaypoint(
    @SerializedName("name") val name: String?,
    @SerializedName("location") val location: List<Double>? // [lng, lat]
)
