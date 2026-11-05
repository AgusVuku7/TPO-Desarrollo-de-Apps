package com.chargeroute.app.data.remote.api

import com.chargeroute.app.data.remote.dto.NominatimResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Busca una direccion escrita por el usuario y devuelve sus coordenadas (OpenStreetMap)
interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") address: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("countrycodes") countryCodes: String = "ar"
    ): Response<List<NominatimResult>>
}
