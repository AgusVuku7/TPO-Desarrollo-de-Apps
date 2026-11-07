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

    // Convierte una estacion de OpenChargeMap al modelo de la app.
    // Devuelve null si la estacion no trae ubicacion.
    fun fromOcmDto(dto: OcmPoiResponse): Station? {
        val addressInfo = dto.addressInfo ?: return null
        val address = listOfNotNull(addressInfo.addressLine1, addressInfo.town, addressInfo.stateOrProvince)
            .filter { it.isNotBlank() }
            .joinToString(", ")

        // Los conectores que la app no conoce se descartan (antes se tomaban como CCS2)
        val connectors = dto.connections.orEmpty().mapNotNull { conn ->
            val type = ocmConnectorType(conn.connectionTypeId, conn.connectionType?.title)
            type?.let { Connector(type = it, powerKw = conn.powerKw ?: 0.0) }
        }.distinctBy { it.type to it.powerKw }

        return Station(
            id = "ocm_${dto.id}",
            remoteId = dto.id.toString(),
            name = addressInfo.title ?: "Estación de carga",
            address = address.ifBlank { "Dirección no informada" },
            latitude = addressInfo.latitude,
            longitude = addressInfo.longitude,
            connectors = connectors,
            availableCount = 0, // OpenChargeMap no informa disponibilidad en vivo
            totalCount = dto.numberOfPoints ?: connectors.size,
            amenities = emptyList(),
            operator = dto.operatorInfo?.title ?: "Operador no informado",
            notes = dto.generalComments ?: "",
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    // OpenChargeMap identifica cada tipo de conector con un numero (ConnectionTypeID).
    // Si el numero no se reconoce, se intenta con el nombre.
    fun ocmConnectorType(connectionTypeId: Int?, title: String?): ConnectorType? {
        when (connectionTypeId) {
            33 -> return ConnectorType.CCS2            // CCS (Type 2)
            32 -> return ConnectorType.CCS1            // CCS (Type 1)
            25, 1036 -> return ConnectorType.TYPE_2    // Type 2 (Mennekes)
            2 -> return ConnectorType.CHADEMO          // CHAdeMO
            27, 30 -> return ConnectorType.TESLA       // Tesla
            28 -> return ConnectorType.SCHUKO          // Schuko
        }
        val name = title ?: return null
        return when {
            name.contains("CCS", ignoreCase = true) && name.contains("Type 1", ignoreCase = true) -> ConnectorType.CCS1
            name.contains("CCS", ignoreCase = true) -> ConnectorType.CCS2
            name.contains("CHAdeMO", ignoreCase = true) -> ConnectorType.CHADEMO
            name.contains("Tesla", ignoreCase = true) -> ConnectorType.TESLA
            name.contains("Type 2", ignoreCase = true) || name.contains("Mennekes", ignoreCase = true) -> ConnectorType.TYPE_2
            name.contains("Schuko", ignoreCase = true) -> ConnectorType.SCHUKO
            else -> null
        }
    }

    private data class ConnectorDto(
        val type: String,
        val powerKw: Double,
        val isCompatible: Boolean
    )
}
