package com.chargeroute.app

import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.StationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationFilterTest {

    // Ruta recta de norte a sur (aprox. 110 km)
    private val route = (0..10).map { i -> Pair(-34.0 - i * 0.1, -58.0) }

    private fun station(id: String, lat: Double, lon: Double, type: ConnectorType) = Station(
        id = id,
        remoteId = id,
        name = id,
        address = "",
        latitude = lat,
        longitude = lon,
        connectors = listOf(Connector(type = type, powerKw = 50.0))
    )

    @Test
    fun keepsOnlyCompatibleStations() {
        val stations = listOf(
            station("ccs", -34.5, -58.0, ConnectorType.CCS2),
            station("chademo", -34.5, -58.0, ConnectorType.CHADEMO)
        )

        val result = StationFilter.alongRoute(stations, route, listOf(ConnectorType.CCS2))

        assertEquals(listOf("ccs"), result.map { it.id })
    }

    @Test
    fun discardsStationsFarFromTheRoute() {
        val stations = listOf(
            station("cerca", -34.5, -58.05, ConnectorType.CCS2), // ~5 km
            station("lejos", -34.5, -58.5, ConnectorType.CCS2)   // ~45 km
        )

        val result = StationFilter.alongRoute(stations, route, listOf(ConnectorType.CCS2))

        assertEquals(listOf("cerca"), result.map { it.id })
    }

    @Test
    fun returnsStationsInTravelOrder() {
        val stations = listOf(
            station("final", -34.9, -58.0, ConnectorType.CCS2),
            station("inicio", -34.1, -58.0, ConnectorType.CCS2),
            station("medio", -34.5, -58.0, ConnectorType.CCS2)
        )

        val result = StationFilter.alongRoute(stations, route, listOf(ConnectorType.CCS2))

        assertEquals(listOf("inicio", "medio", "final"), result.map { it.id })
    }

    @Test
    fun emptyRouteReturnsNoStations() {
        val stations = listOf(station("a", -34.5, -58.0, ConnectorType.CCS2))

        assertTrue(StationFilter.alongRoute(stations, emptyList(), listOf(ConnectorType.CCS2)).isEmpty())
    }

    @Test
    fun simplifyKeepsFirstAndLastPoint() {
        val longRoute = (0..999).map { i -> Pair(i.toDouble(), 0.0) }

        val result = StationFilter.simplify(longRoute, 300)

        assertTrue(result.size <= 301)
        assertEquals(longRoute.first(), result.first())
        assertEquals(longRoute.last(), result.last())
    }
}
