package com.chargeroute.app

import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.domain.model.UsedChargers
import com.chargeroute.app.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsedChargersTest {

    private fun station(id: String) = Station(
        id = id, remoteId = id, name = id, address = "", latitude = 0.0, longitude = 0.0,
        connectors = listOf(Connector(ConnectorType.CCS2, 50.0))
    )

    private fun trip(id: String, createdAt: Long, vararg stations: String) = Trip(
        id = id,
        origin = LocationPoint("Obelisco, Buenos Aires", "", 0.0, 0.0),
        destination = LocationPoint("Córdoba, Argentina", "", 0.0, 0.0),
        vehicle = Vehicle(
            id = "v", brand = "B", model = "M", year = 2024, version = "V",
            usableBatteryKwh = 60.0, wltpRangeKm = 400.0,
            supportedConnectors = listOf(ConnectorType.CCS2), maxDcKw = 100.0
        ),
        initialBatteryPercent = 80, currentBatteryPercent = 80, acActive = false,
        routePlan = RoutePlan(
            origin = LocationPoint("A", "", 0.0, 0.0),
            destination = LocationPoint("B", "", 0.0, 0.0),
            distanceKm = 100.0, totalDurationMinutes = 0, driveDurationMinutes = 0, chargeDurationMinutes = 0,
            stops = stations.mapIndexed { i, s -> TripStop("$id-s$i", station(s), i, 10, 20, 80, 10.0) },
            polylinePoints = emptyList(), encodedPolyline = "",
            arrivalBatteryPercent = 50, consumptionKwhPer100Km = 15.0
        ),
        createdAtMillis = createdAt
    )

    @Test
    fun withoutStopsThereAreNoChargers() {
        assertTrue(UsedChargers.fromTrips(listOf(trip("t1", 1))).isEmpty())
    }

    @Test
    fun repeatedChargerAppearsOnceWithTheLatestTrip() {
        val chargers = UsedChargers.fromTrips(
            listOf(trip("viejo", 100, "ypf"), trip("nuevo", 200, "ypf", "shell"))
        )

        assertEquals(2, chargers.size)
        val ypf = chargers.first { it.station.id == "ypf" }
        assertEquals(2, ypf.tripsCount)
        assertEquals("nuevo", ypf.lastTripId)
        assertEquals("nuevo-s0", ypf.lastStopId)
        assertEquals("Obelisco → Córdoba", ypf.lastTripLabel)
    }

    @Test
    fun mostRecentlyUsedComeFirst() {
        val chargers = UsedChargers.fromTrips(
            listOf(trip("viejo", 100, "axion"), trip("nuevo", 200, "shell"))
        )

        assertEquals(listOf("shell", "axion"), chargers.map { it.station.id })
    }
}
