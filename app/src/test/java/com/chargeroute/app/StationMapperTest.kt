package com.chargeroute.app

import com.chargeroute.app.data.mapper.StationMapper
import com.chargeroute.app.data.remote.dto.OcmAddressInfo
import com.chargeroute.app.data.remote.dto.OcmConnection
import com.chargeroute.app.data.remote.dto.OcmConnectionType
import com.chargeroute.app.data.remote.dto.OcmPoiResponse
import com.chargeroute.app.domain.model.ConnectorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationMapperTest {

    private fun connection(typeId: Int?, title: String? = null, kw: Double? = 50.0) =
        OcmConnection(
            connectionTypeId = typeId,
            connectionType = title?.let { OcmConnectionType(title = it, formalName = null) },
            powerKw = kw,
            currentTypeId = null,
            quantity = 1
        )

    private fun poi(connections: List<OcmConnection>, address: OcmAddressInfo? = defaultAddress) =
        OcmPoiResponse(
            id = 123,
            addressInfo = address,
            connections = connections,
            numberOfPoints = 2,
            operatorInfo = null,
            generalComments = null,
            dateLastStatusUpdate = null
        )

    private val defaultAddress = OcmAddressInfo(
        title = "Estación Test",
        addressLine1 = "Ruta 2 km 100",
        town = "Chascomús",
        stateOrProvince = "Buenos Aires",
        latitude = -35.5,
        longitude = -58.0
    )

    @Test
    fun mapsKnownConnectionTypeIds() {
        assertEquals(ConnectorType.CCS2, StationMapper.ocmConnectorType(33, null))
        assertEquals(ConnectorType.TYPE_2, StationMapper.ocmConnectorType(25, null))
        assertEquals(ConnectorType.CHADEMO, StationMapper.ocmConnectorType(2, null))
    }

    @Test
    fun usesTitleWhenIdIsUnknown() {
        assertEquals(ConnectorType.CCS1, StationMapper.ocmConnectorType(null, "CCS (Type 1)"))
        assertEquals(ConnectorType.CCS2, StationMapper.ocmConnectorType(null, "CCS (Type 2)"))
    }

    @Test
    fun unknownConnectorIsNotTreatedAsCcs2() {
        // Antes cualquier conector desconocido se convertia en CCS2 y parecia compatible
        assertNull(StationMapper.ocmConnectorType(1, "Type 1 (J1772)"))
    }

    @Test
    fun stationWithoutAddressIsDiscarded() {
        assertNull(StationMapper.fromOcmDto(poi(listOf(connection(33)), address = null)))
    }

    @Test
    fun mapsStationDataWithoutInventingValues() {
        val station = StationMapper.fromOcmDto(poi(listOf(connection(33, kw = 60.0), connection(1))))!!

        assertEquals("ocm_123", station.id)
        assertEquals(listOf(ConnectorType.CCS2), station.connectors.map { it.type })
        assertEquals(60.0, station.connectors.first().powerKw, 0.0)
        assertEquals(0, station.availableCount)
        assertEquals(emptyList<String>(), station.amenities)
    }
}
