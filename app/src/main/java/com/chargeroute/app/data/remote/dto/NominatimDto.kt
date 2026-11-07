package com.chargeroute.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NominatimResult(
    @SerializedName("lat") val lat: String?,
    @SerializedName("lon") val lon: String?,
    @SerializedName("display_name") val displayName: String?,
    // Tipo de lugar: "city", "town", "road", "state" (provincia), "country", etc.
    @SerializedName("addresstype") val addressType: String? = null
)
