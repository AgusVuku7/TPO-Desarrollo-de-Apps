package com.chargeroute.app

import com.chargeroute.app.data.mapper.RouteMapper
import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMapperTest {

    private val origin = LocationPoint("Origen", "Calle 1", -34.0, -58.0)
    private val destination = LocationPoint("Destino", "Calle 2", -35.0, -57.0)
    private val points = listOf(Pair(-34.0, -58.0), Pair(-34.5, -57.5), Pair(-35.0, -57.0))

    private val station = Station(
        id = "s1", remoteId = "r1", name = "Estacion", address = "Ruta 2",
        latitude = -34.5, longitude = -57.5,
        connectors = listOf(Connector(ConnectorType.CCS2, 50.0)),
        availableCount = 1, totalCount = 2, amenities = emptyList(),
        operator = "Op", notes = "", lastUpdatedMillis = 0L
    )

    private val stop = TripStop(
        id = "stop_1", station = station, orderIndex = 0, stopDurationMinutes = 30,
        arrivalBatteryPercent = 25, departureBatteryPercent = 80, chargedEnergyKwh = 27.5,
        arrivalTime = "10:00", departureTime = "10:30"
    )

    private fun plan(stops: List<TripStop> = emptyList()) = RoutePlan(
        origin = origin,
        destination = destination,
        distanceKm = 200.0,
        totalDurationMinutes = 150,
        driveDurationMinutes = 120,
        chargeDurationMinutes = 30,
        stops = stops,
        polylinePoints = points,
        encodedPolyline = encodePolyline(points),
        arrivalBatteryPercent = 40,
        consumptionKwhPer100Km = 17.1,
        calculatedAtMillis = 0L,
        directDistanceKm = 190.0
    )

    @Test
    fun decodesTheOfficialGoogleExample() {
        val decoded = RouteMapper.decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, decoded.size)
        assertEquals(38.5, decoded[0].first, 0.00001)
        assertEquals(-120.2, decoded[0].second, 0.00001)
        assertEquals(40.7, decoded[1].first, 0.00001)
        assertEquals(-120.95, decoded[1].second, 0.00001)
        assertEquals(43.252, decoded[2].first, 0.00001)
        assertEquals(-126.453, decoded[2].second, 0.00001)
    }

    @Test
    fun emptyTextDecodesToEmptyList() {
        assertTrue(RouteMapper.decodePolyline("").isEmpty())
    }

    @Test
    fun toEntityKeepsTheNumbersOfThePlan() {
        val entity = RouteMapper.toEntity("trip_1", plan())
        assertEquals("route_trip_1", entity.id)
        assertEquals("trip_1", entity.tripId)
        assertEquals(200.0, entity.distanceKm, 0.0001)
        assertEquals(150, entity.totalDurationMinutes)
        assertEquals(40, entity.arrivalBatteryPercent)
        assertEquals(17.1, entity.consumptionKwhPer100Km, 0.0001)
        assertEquals(190.0, entity.directDistanceKm, 0.0001)
    }

    @Test
    fun planSavedAndLoadedKeepsDistanceDurationBatteryAndConsumption() {
        val entity = RouteMapper.toEntity("trip_1", plan())
        val loaded = RouteMapper.fromEntities(origin, destination, entity, emptyList(), emptyMap())

        assertEquals(200.0, loaded.distanceKm, 0.0001)
        assertEquals(150, loaded.totalDurationMinutes)
        assertEquals(120, loaded.driveDurationMinutes)
        assertEquals(30, loaded.chargeDurationMinutes)
        assertEquals(40, loaded.arrivalBatteryPercent)
        assertEquals(17.1, loaded.consumptionKwhPer100Km, 0.0001)
        assertEquals(190.0, loaded.directDistanceKm, 0.0001)
        assertEquals(0L, loaded.calculatedAtMillis)
    }

    @Test
    fun polylineSurvivesTheRoundTrip() {
        val entity = RouteMapper.toEntity("trip_1", plan())
        val loaded = RouteMapper.fromEntities(origin, destination, entity, emptyList(), emptyMap())

        assertEquals(points.size, loaded.polylinePoints.size)
        for (i in points.indices) {
            assertEquals(points[i].first, loaded.polylinePoints[i].first, 0.00001)
            assertEquals(points[i].second, loaded.polylinePoints[i].second, 0.00001)
        }
    }

    @Test
    fun stopSurvivesTheRoundTrip() {
        val entity = RouteMapper.toEntity("trip_1", plan(listOf(stop)))
        val stopEntities = RouteMapper.toStopEntities(entity.id, listOf(stop))
        assertEquals("s1", stopEntities[0].stationId)
        assertEquals(entity.id, stopEntities[0].routeId)

        val loaded = RouteMapper.fromEntities(origin, destination, entity, stopEntities, mapOf("s1" to station))
        assertEquals(1, loaded.stops.size)
        assertEquals(stop, loaded.stops[0])
    }

    @Test
    fun stopWhoseStationIsMissingIsDropped() {
        // Comportamiento actual: si la estacion no esta en el mapa, la parada se descarta sin avisar
        val entity = RouteMapper.toEntity("trip_1", plan(listOf(stop)))
        val stopEntities = RouteMapper.toStopEntities(entity.id, listOf(stop))

        val loaded = RouteMapper.fromEntities(origin, destination, entity, stopEntities, emptyMap())
        assertTrue(loaded.stops.isEmpty())
    }
}
