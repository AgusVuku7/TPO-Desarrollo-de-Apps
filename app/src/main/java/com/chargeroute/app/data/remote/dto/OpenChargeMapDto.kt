package com.chargeroute.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OcmPoiResponse(
    @SerializedName("ID") val id: Long,
    @SerializedName("AddressInfo") val addressInfo: OcmAddressInfo?,
    @SerializedName("Connections") val connections: List<OcmConnection>?,
    @SerializedName("NumberOfPoints") val numberOfPoints: Int?,
    @SerializedName("OperatorInfo") val operatorInfo: OcmOperatorInfo?,
    @SerializedName("GeneralComments") val generalComments: String?,
    @SerializedName("DateLastStatusUpdate") val dateLastStatusUpdate: String?
)

data class OcmAddressInfo(
    @SerializedName("Title") val title: String?,
    @SerializedName("AddressLine1") val addressLine1: String?,
    @SerializedName("Town") val town: String?,
    @SerializedName("StateOrProvince") val stateOrProvince: String?,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double
)

data class OcmConnection(
    @SerializedName("ConnectionTypeID") val connectionTypeId: Int?,
    @SerializedName("ConnectionType") val connectionType: OcmConnectionType?,
    @SerializedName("PowerKW") val powerKw: Double?,
    @SerializedName("CurrentTypeID") val currentTypeId: Int?,
    @SerializedName("Quantity") val quantity: Int?
)

data class OcmConnectionType(
    @SerializedName("Title") val title: String?,
    @SerializedName("FormalName") val formalName: String?
)

data class OcmOperatorInfo(
    @SerializedName("Title") val title: String?
)
