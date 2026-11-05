package com.chargeroute.app.data.remote.api

import com.chargeroute.app.data.remote.dto.OsrmRouteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OsrmRoutingApi {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String, // "lng1,lat1;lng2,lat2"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("steps") steps: Boolean = false
    ): Response<OsrmRouteResponse>
}
