package com.chargeroute.app.data.mapper

import com.chargeroute.app.data.local.entity.StationEntity
import com.chargeroute.app.data.remote.dto.OcmPoiResponse
import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Station
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object StationMapper {

    private val gson = Gson()

    fun fromEntity(entity: StationEntity): Station {
        val connectorListType = object : TypeToken<List<ConnectorDto>>() {}.type
        val connectorDtos: List<ConnectorDto> = try {
            gson.fromJson(entity.connectorsJson, connectorListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val amenitiesType = object : TypeToken<List<String>>() {}.type
        val amenities: List<String> = try {
            gson.fromJson(entity.amenitiesJson, amenitiesType) ?: listOf("24 h", "Café", "Baños")
        } catch (_: Exception) {
            listOf("24 h", "Café", "Baños")
        }

        val domainConnectors = connectorDtos.map { dto ->
            Connector(
                type = ConnectorType.fromString(dto.type),
                powerKw = dto.powerKw,
                isCompatible = dto.isCompatible
            )
        }

        return Station(
            id = entity.id,
            remoteId = entity.remoteId,
            name = entity.name,
            address = entity.address,
            latitude = entity.latitude,
            longitude = entity.longitude,
            connectors = domainConnectors,
            availableCount = entity.availableCount,
            totalCount = entity.totalCount,
            amenities = amenities,
            operator = entity.operator,
            notes = entity.notes,
            lastUpdatedMillis = entity.lastUpdatedMillis
        )
    }

    fun toEntity(station: Station): StationEntity {
        val connectorDtos = station.connectors.map {
            ConnectorDto(type = it.type.name, powerKw = it.powerKw, isCompatible = it.isCompatible)
        }
        return StationEntity(
            id = station.id,
            remoteId = station.remoteId,
            name = station.name,
            address = station.address,
            latitude = station.latitude,
            longitude = station.longitude,
            connectorsJson = gson.toJson(connectorDtos),
            availableCount = station.availableCount,
            totalCount = station.totalCount,
            amenitiesJson = gson.toJson(station.amenities),
            operator = station.operator,
            notes = station.notes,
            lastUpdatedMillis = station.lastUpdatedMillis
        )
    }

    fun fromOcmDto(dto: OcmPoiResponse): Station {
        val addressInfo = dto.addressInfo
        val name = addressInfo?.title ?: "Estación de carga"
        val address = listOfNotNull(addressInfo?.addressLine1, addressInfo?.town, addressInfo?.stateOrProvince).joinToString(", ")
        val lat = addressInfo?.latitude ?: 0.0
        val lng = addressInfo?.longitude ?: 0.0

        val connectors = dto.connections?.mapNotNull { conn ->
            val typeTitle = conn.connectionType?.title ?: conn.connectionType?.formalName ?: "CCS2"
            val type = when {
                typeTitle.contains("CCS", ignoreCase = true) -> ConnectorType.CCS2
                typeTitle.contains("Type 2", ignoreCase = true) || typeTitle.contains("Mennekes", ignoreCase = true) -> ConnectorType.TYPE_2
                typeTitle.contains("CHAdeMO", ignoreCase = true) -> ConnectorType.CHADEMO
                typeTitle.contains("Tesla", ignoreCase = true) -> ConnectorType.TESLA
                else -> ConnectorType.CCS2
            }
            Connector(type = type, powerKw = conn.powerKw ?: 50.0, isCompatible = true)
        } ?: listOf(Connector(type = ConnectorType.CCS2, powerKw = 120.0))

        return Station(
            id = "ocm_${dto.id}",
            remoteId = dto.id.toString(),
            name = name,
            address = if (address.isNotBlank()) address else "Ruta / Autovía",
            latitude = lat,
            longitude = lng,
            connectors = connectors,
            availableCount = 2,
            totalCount = dto.numberOfPoints ?: 4,
            amenities = listOf("24 h", "Café", "Baños"),
            operator = dto.operatorInfo?.title ?: "Red de Carga",
            notes = dto.generalComments ?: "Confirmá disponibilidad y conector antes de llegar.",
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    private data class ConnectorDto(
        val type: String,
        val powerKw: Double,
        val isCompatible: Boolean
    )
}
