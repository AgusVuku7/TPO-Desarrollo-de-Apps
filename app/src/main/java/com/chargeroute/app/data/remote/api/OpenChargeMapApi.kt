package com.chargeroute.app.data.remote.api

import com.chargeroute.app.data.remote.dto.OcmPoiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenChargeMapApi {
    @GET("v3/poi/")
    suspend fun getChargingStations(
        @Query("output") output: String = "json",
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distanceKm: Double = 50.0,
        @Query("distanceunit") distanceUnit: String = "KM",
        @Query("maxresults") maxResults: Int = 20,
        @Query("key") apiKey: String = "open-key"
    ): Response<List<OcmPoiResponse>>
}
