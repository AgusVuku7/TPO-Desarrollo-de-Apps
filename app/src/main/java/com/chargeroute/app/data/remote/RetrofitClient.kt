package com.chargeroute.app.data.remote

import com.chargeroute.app.data.remote.api.NominatimApi
import com.chargeroute.app.data.remote.api.OpenChargeMapApi
import com.chargeroute.app.data.remote.api.OsrmRoutingApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val OSRM_BASE_URL = "https://router.project-osrm.org/"
    private const val OCM_BASE_URL = "https://api.openchargemap.io/"
    private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            // El servidor de rutas es publico y a veces tarda: con 5 segundos se caia a la linea recta
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    val osrmApi: OsrmRoutingApi by lazy {
        Retrofit.Builder()
            .baseUrl(OSRM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OsrmRoutingApi::class.java)
    }

    // Nominatim pide que cada app se identifique con su propio User-Agent
    val nominatimApi: NominatimApi by lazy {
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RouteCharge-TPO/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
        Retrofit.Builder()
            .baseUrl(NOMINATIM_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }

    val ocmApi: OpenChargeMapApi by lazy {
        Retrofit.Builder()
            .baseUrl(OCM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenChargeMapApi::class.java)
    }
}
