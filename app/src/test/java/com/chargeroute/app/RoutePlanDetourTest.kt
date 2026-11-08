package com.chargeroute.app

import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutePlanDetourTest {

    private fun plan(distanceKm: Double, directDistanceKm: Double) = RoutePlan(
        origin = LocationPoint("Obelisco", "", -34.6037, -58.3816),
        destination = LocationPoint("Mar del Plata", "", -38.0055, -57.5432),
        distanceKm = distanceKm,
        totalDurationMinutes = 0,
        driveDurationMinutes = 0,
        chargeDurationMinutes = 0,
        stops = emptyList(),
        polylinePoints = emptyList(),
        encodedPolyline = "",
        arrivalBatteryPercent = 50,
        consumptionKwhPer100Km = 15.0,
        directDistanceKm = directDistanceKm
    )

    @Test
    fun detourIsTheDifferenceWithTheDirectRoute() {
        // Caso real: 414 km directo y 431 km pasando por 4 cargadores
        assertEquals(17, plan(431.0, 414.0).detourKm)
    }

    @Test
    fun withoutDirectDistanceThereIsNoDetour() {
        // Rutas guardadas antes de la migracion (0 = sin dato)
        assertEquals(0, plan(431.0, 0.0).detourKm)
    }

    @Test
    fun detourIsNeverNegative() {
        assertEquals(0, plan(410.0, 414.0).detourKm)
    }
}
