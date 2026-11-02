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
    @SerializedName("weight") val weight: Double?
)

data class OsrmWaypoint(
    @SerializedName("name") val name: String?,
    @SerializedName("location") val location: List<Double>? // [lng, lat]
)
