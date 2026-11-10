package com.chargeroute.app

import com.chargeroute.app.data.mapper.RouteMapper
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeRouteLogicTest {

    private val testVehicle = Vehicle(
        id = "hyundai_kona_2023",
        brand = "Hyundai",
        model = "Kona Electric",
        year = 2023,
        version = "Long Range - 64 kWh",
        usableBatteryKwh = 64.0,
        wltpRangeKm = 484.0,
        supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
        maxDcKw = 77.0,
        baseConsumptionKwhPer100Km = 15.5
    )

    @Test
    fun testVehicleSpecs() {
        assertEquals("Hyundai Kona Electric (Long Range - 64 kWh)", testVehicle.displayName)
        assertTrue(testVehicle.supportedConnectors.contains(ConnectorType.CCS2))
        assertTrue(testVehicle.supportedConnectors.contains(ConnectorType.TYPE_2))
        assertFalse(testVehicle.supportedConnectors.contains(ConnectorType.CHADEMO))
    }

    @Test
    fun testClimateControlConsumptionImpact() {
        val baseConsumption = testVehicle.baseConsumptionKwhPer100Km
        val withAc = baseConsumption * 1.10
        assertTrue("AC should increase energy consumption by 10%", withAc > baseConsumption)
        assertEquals(17.05, withAc, 0.01)
    }

    @Test
    fun testPolylineDecoder() {
        // Standard Google/OSRM encoded polyline test
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = RouteMapper.decodePolyline(encoded)
        assertNotNull(points)
        assertTrue(points.isNotEmpty())
    }

    @Test
    fun testBatteryClamping() {
        val lowPercent = -10
        val highPercent = 120
        val normalPercent = 70

        assertEquals(0, lowPercent.coerceIn(0, 100))
        assertEquals(100, highPercent.coerceIn(0, 100))
        assertEquals(70, normalPercent.coerceIn(0, 100))
    }
}
