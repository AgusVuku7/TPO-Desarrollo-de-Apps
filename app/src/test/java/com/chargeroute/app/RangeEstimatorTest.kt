package com.chargeroute.app

import com.chargeroute.app.domain.model.RangeEstimator
import com.chargeroute.app.domain.model.TripClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RangeEstimatorTest {

    // Auto de 50 kWh que gasta 20 kWh/100 km => 0,4 % por km, 250 km con el 100 %

    @Test
    fun rangeWithFullBattery() {
        assertEquals(250.0, RangeEstimator.rangeKm(100, 50.0, 20.0), 0.01)
        assertEquals(125.0, RangeEstimator.rangeKm(50, 50.0, 20.0), 0.01)
    }

    @Test
    fun reachesTargetWithReserve() {
        // 100 km gastan 40 %: con 80 % llega con 40 %
        val estimate = RangeEstimator.estimate("Dolores", 100.0, 80, 50.0, 20.0)

        assertEquals(40, estimate.arrivalBatteryPercent)
        assertTrue(estimate.reaches)
        assertTrue(estimate.reachesWithReserve)
    }

    @Test
    fun reachesJustWithoutReserve() {
        // Con 50 % llega con 10 %: llega, pero sin la reserva del 15 %
        val estimate = RangeEstimator.estimate("Dolores", 100.0, 50, 50.0, 20.0)

        assertEquals(10, estimate.arrivalBatteryPercent)
        assertTrue(estimate.reaches)
        assertFalse(estimate.reachesWithReserve)
    }

    @Test
    fun doesNotReachAndSaysHowMuchIsMissing() {
        // Con 30 % le faltan 10 % para hacer los 100 km
        val estimate = RangeEstimator.estimate("Dolores", 100.0, 30, 50.0, 20.0)

        assertFalse(estimate.reaches)
        assertEquals(-10, estimate.arrivalBatteryPercent)
        assertEquals(75, estimate.rangeKm)
    }

    @Test
    fun tripClockAddsMinutesToDeparture() {
        val zone = ZoneId.of("America/Argentina/Buenos_Aires")
        val departure = LocalDateTime.of(2026, 11, 8, 10, 20).atZone(zone).toInstant().toEpochMilli()

        assertEquals("10:20", TripClock.at(departure, 0, zone))
        assertEquals("11:55", TripClock.at(departure, 95, zone))
        assertEquals("00:10", TripClock.at(departure, 830, zone)) // pasa la medianoche
    }
}
