package com.chargeroute.app.data.remote

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

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
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

    val ocmApi: OpenChargeMapApi by lazy {
        Retrofit.Builder()
            .baseUrl(OCM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenChargeMapApi::class.java)
    }
}
