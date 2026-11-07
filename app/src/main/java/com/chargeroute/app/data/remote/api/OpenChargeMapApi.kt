package com.chargeroute.app.data.remote.api

import com.chargeroute.app.data.remote.dto.OcmPoiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenChargeMapApi {
    // Devuelve las estaciones que estan dentro de un rectangulo.
    // boundingbox tiene el formato "(lat1,lon1),(lat2,lon2)" (dos esquinas opuestas).
    @GET("v3/poi/")
    suspend fun getStationsInArea(
        @Query("boundingbox") boundingBox: String,
        @Query("key") apiKey: String,
        @Query("output") output: String = "json",
        @Query("maxresults") maxResults: Int = 500,
        @Query("verbose") verbose: Boolean = false
    ): Response<List<OcmPoiResponse>>
}
