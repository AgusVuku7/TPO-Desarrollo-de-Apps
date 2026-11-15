package com.chargeroute.app

import com.chargeroute.app.domain.model.StopSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopScheduleTest {

    // Ruta de 200 km que se maneja en 120 minutos

    @Test
    fun secondStopAddsTheFirstCharge() {
        // Parada a los 50 km: 50/200 * 120 = 30 min
        // Parada a los 150 km: 150/200 * 120 = 90 min + 20 min de la primera carga = 110
        val minutes = StopSchedule.minutesToEachStop(listOf(50.0, 150.0), listOf(20, 15), 200.0, 120)

        assertEquals(listOf(30, 110), minutes)
    }

    @Test
    fun noStopsGivesEmptyList() {
        assertTrue(StopSchedule.minutesToEachStop(emptyList(), emptyList(), 200.0, 120).isEmpty())
    }

    @Test
    fun zeroRouteKmGivesZeroDriveMinutes() {
        // Sin distancia no se divide: solo cuentan las cargas anteriores
        val minutes = StopSchedule.minutesToEachStop(listOf(10.0, 20.0), listOf(25, 10), 0.0, 120)

        assertEquals(listOf(0, 25), minutes)
    }
}
