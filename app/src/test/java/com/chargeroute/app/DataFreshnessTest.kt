package com.chargeroute.app

import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.DataFreshness
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DataFreshnessTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    private fun station(id: String, updated: Long) = Station(
        id = id, remoteId = id, name = id, address = "", latitude = 0.0, longitude = 0.0,
        connectors = listOf(Connector(ConnectorType.CCS2, 50.0)), lastUpdatedMillis = updated
    )

    private fun plan(stops: List<Station>, nearby: List<Station>) = RoutePlan(
        origin = LocationPoint("A", "", 0.0, 0.0),
        destination = LocationPoint("B", "", 0.0, 0.0),
        distanceKm = 100.0, totalDurationMinutes = 0, driveDurationMinutes = 0, chargeDurationMinutes = 0,
        stops = stops.mapIndexed { i, s -> TripStop("s$i", s, i, 10, 20, 80, 10.0) },
        nearbyStations = nearby,
        polylinePoints = emptyList(), encodedPolyline = "",
        arrivalBatteryPercent = 50, consumptionKwhPer100Km = 15.0
    )

    @Test
    fun usesTheOldestStationDate() {
        val p = plan(listOf(station("parada", now - 2 * day)), listOf(station("cercana", now - 10 * day)))

        assertEquals(now - 10 * day, DataFreshness.oldestStationUpdate(p))
    }

    @Test
    fun withoutStationsThereIsNoDate() {
        assertNull(DataFreshness.oldestStationUpdate(plan(emptyList(), emptyList())))
    }

    @Test
    fun dataOlderThan30DaysIsStale() {
        assertFalse(DataFreshness.isStale(now - 29 * day, now))
        assertTrue(DataFreshness.isStale(now - 31 * day, now))
    }

    @Test
    fun formatsTheDate() {
        val zone = ZoneId.of("America/Argentina/Buenos_Aires")
        val millis = LocalDate.of(2026, 11, 7).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals("07/11/2026", DataFreshness.formatDate(millis, zone))
    }
}
