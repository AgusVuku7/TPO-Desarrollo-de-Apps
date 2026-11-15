package com.chargeroute.app

import com.chargeroute.app.domain.model.TripEnergy
import org.junit.Assert.assertEquals
import org.junit.Test

class TripEnergyTest {

    // Auto de 50 kWh que gasta 20 kWh/100 km

    @Test
    fun usedKwhIsDistanceTimesConsumption() {
        // 100 km gastan 20 kWh, 250 km gastan 50 kWh
        assertEquals(20.0, TripEnergy.usedKwh(100.0, 20.0), 0.01)
        assertEquals(50.0, TripEnergy.usedKwh(250.0, 20.0), 0.01)
    }

    @Test
    fun usedKwhWithoutDistanceIsZero() {
        assertEquals(0.0, TripEnergy.usedKwh(0.0, 20.0), 0.01)
    }

    @Test
    fun remainingKwhIsPercentOfBattery() {
        assertEquals(50.0, TripEnergy.remainingKwh(100, 50.0), 0.01)
        assertEquals(12.5, TripEnergy.remainingKwh(25, 50.0), 0.01)
    }

    @Test
    fun remainingKwhWithEmptyBatteryIsZero() {
        assertEquals(0.0, TripEnergy.remainingKwh(0, 50.0), 0.01)
    }
}
