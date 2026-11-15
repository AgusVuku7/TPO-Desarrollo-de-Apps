package com.chargeroute.app

import com.chargeroute.app.data.mapper.TripMapper
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMapperTest {

    private val origin = LocationPoint("Origen", "Calle 1", -34.6, -58.4)
    private val destination = LocationPoint("Destino", "Calle 2", -38.0, -57.5)

    private fun vehicle(connectors: List<ConnectorType>) = Vehicle(
        id = "kona", brand = "Hyundai", model = "Kona", year = 2023, version = "64 kWh",
        usableBatteryKwh = 64.0, wltpRangeKm = 484.0,
        supportedConnectors = connectors, maxDcKw = 77.0
    )

    private val points = listOf(Pair(-34.6, -58.4), Pair(-38.0, -57.5))

    private val plan = RoutePlan(
        origin = origin, destination = destination,
        distanceKm = 400.0, totalDurationMinutes = 300, driveDurationMinutes = 270,
        chargeDurationMinutes = 30, stops = emptyList(),
        polylinePoints = points, encodedPolyline = encodePolyline(points),
        arrivalBatteryPercent = 30, consumptionKwhPer100Km = 15.5, calculatedAtMillis = 0L
    )

    private fun trip(connectors: List<ConnectorType> = listOf(ConnectorType.TYPE_2, ConnectorType.CCS2)) = Trip(
        id = "trip_1", origin = origin, destination = destination,
        vehicle = vehicle(connectors),
        initialBatteryPercent = 90, currentBatteryPercent = 65, acActive = true,
        routePlan = plan, createdAtMillis = 1000L, isCompleted = false
    )

    @Test
    fun toEntityKeepsIdBatteryAndAirFlag() {
        val entity = TripMapper.toEntity(trip())
        assertEquals("trip_1", entity.id)
        assertEquals(90, entity.initialBatteryPercent)
        assertEquals(65, entity.currentBatteryPercent)
        assertTrue(entity.acActive)
        assertFalse(entity.isCompleted)
        assertEquals(1000L, entity.createdAtMillis)
    }

    @Test
    fun toEntityKeepsOriginAndDestination() {
        val entity = TripMapper.toEntity(trip())
        assertEquals("Origen", entity.originName)
        assertEquals(-34.6, entity.originLat, 0.00001)
        assertEquals(-58.4, entity.originLng, 0.00001)
        assertEquals("Destino", entity.destName)
        assertEquals(-38.0, entity.destLat, 0.00001)
        assertEquals(-57.5, entity.destLng, 0.00001)
    }

    @Test
    fun toEntitySavesTheVehicleData() {
        val entity = TripMapper.toEntity(trip())
        assertEquals("kona", entity.vehicleId)
        assertEquals("Hyundai Kona (64 kWh)", entity.vehicleDisplayName)
        assertEquals(484.0, entity.vehicleRangeKm, 0.0001)
        // Se guarda solo el primer conector de la lista
        assertEquals("TYPE_2", entity.vehicleConnector)
    }

    @Test
    fun vehicleWithoutConnectorsIsSavedEmpty() {
        // Sin conectores no se inventa ninguno: queda vacio
        assertEquals("", TripMapper.toEntity(trip(emptyList())).vehicleConnector)
    }

    @Test
    fun fromEntityRestoresTheTrip() {
        val original = trip()
        val restored = TripMapper.fromEntity(TripMapper.toEntity(original), original.vehicle, plan)
        assertEquals(original, restored)
    }
}
