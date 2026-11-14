package com.chargeroute.app

import com.chargeroute.app.domain.model.ChargingPlanner
import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingPlannerEdgeTest {

    @Test
    fun noMinutesWhenFinalPercentEqualsInitial() {
        assertEquals(0, ChargingPlanner.chargeMinutes(60.0, 60.0, 50.0, 50.0))
    }

    @Test
    fun noMinutesWhenFinalPercentIsLowerThanInitial() {
        assertEquals(0, ChargingPlanner.chargeMinutes(90.0, 70.0, 50.0, 50.0))
    }

    @Test
    fun verySmallChargeTakesTheFiveMinuteMinimum() {
        // Comportamiento actual: el minimo es 5 minutos.
        // De 50 % a 51 %: 1 % de 50 kWh = 0,5 kWh a 50 kW = 0,6 min => se redondea a 1, pero el minimo lo sube a 5
        assertEquals(5, ChargingPlanner.chargeMinutes(50.0, 51.0, 50.0, 50.0))
    }

    @Test
    fun chargeJustOverTheMinimumIsNotRaised() {
        // De 50 % a 60 %: 10 % de 50 kWh = 5 kWh a 50 kW = 0,1 h = 6 min (mayor que 5, queda 6)
        assertEquals(6, ChargingPlanner.chargeMinutes(50.0, 60.0, 50.0, 50.0))
    }
}
