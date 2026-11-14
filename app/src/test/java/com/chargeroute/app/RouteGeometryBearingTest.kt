package com.chargeroute.app

import com.chargeroute.app.domain.model.RouteGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGeometryBearingTest {

    // Ruta de dos puntos cerca del ecuador: 1 grado de diferencia en una sola direccion
    private val north = listOf(Pair(0.0, 0.0), Pair(1.0, 0.0))
    private val east = listOf(Pair(0.0, 0.0), Pair(0.0, 1.0))
    private val south = listOf(Pair(1.0, 0.0), Pair(0.0, 0.0))
    private val west = listOf(Pair(0.0, 1.0), Pair(0.0, 0.0))

    @Test
    fun routeGoingNorthPointsToZeroDegrees() {
        assertEquals(0.0, RouteGeometry.bearingAtProgress(north, 0.5f).toDouble(), 0.5)
    }

    @Test
    fun routeGoingEastPointsToNinetyDegrees() {
        assertEquals(90.0, RouteGeometry.bearingAtProgress(east, 0.5f).toDouble(), 0.5)
    }

    @Test
    fun routeGoingSouthPointsToOneHundredEightyDegrees() {
        assertEquals(180.0, RouteGeometry.bearingAtProgress(south, 0.5f).toDouble(), 0.5)
    }

    @Test
    fun routeGoingWestPointsToTwoHundredSeventyDegrees() {
        assertEquals(270.0, RouteGeometry.bearingAtProgress(west, 0.5f).toDouble(), 0.5)
    }

    @Test
    fun bearingAtTheEndOfTheRouteKeepsTheDirection() {
        // Al final no hay "adelante": mira los ultimos 100 m y sigue dando este
        assertEquals(90.0, RouteGeometry.bearingAtProgress(east, 1f).toDouble(), 0.5)
    }

    @Test
    fun bearingWithLessThanTwoPointsIsZero() {
        assertEquals(0f, RouteGeometry.bearingAtProgress(emptyList(), 0.5f), 0.0001f)
        assertEquals(0f, RouteGeometry.bearingAtProgress(listOf(Pair(1.0, 1.0)), 0.5f), 0.0001f)
    }

    @Test
    fun oneDegreeOfLatitudeIsAbout111Km() {
        // 6371 km * (pi / 180) = 111,19 km
        assertEquals(111.0, RouteGeometry.distanceKm(0.0, 0.0, 1.0, 0.0), 1.0)
    }

    @Test
    fun distanceFromAPointToItselfIsZero() {
        assertEquals(0.0, RouteGeometry.distanceKm(-34.6037, -58.3816, -34.6037, -58.3816), 0.0001)
    }

    @Test
    fun pointAtProgressWithEmptyListReturnsTheObelisco() {
        // Comportamiento actual: con lista vacia devuelve las coordenadas fijas del Obelisco
        val point = RouteGeometry.pointAtProgress(emptyList(), 0.5f)
        assertEquals(-34.6037, point.first, 0.0001)
        assertEquals(-58.3816, point.second, 0.0001)
    }

    @Test
    fun pointAtProgressWithOnePointReturnsThatPoint() {
        val only = Pair(-35.0, -58.0)
        assertEquals(only, RouteGeometry.pointAtProgress(listOf(only), 0.5f))
    }

    @Test
    fun nearestOnRouteWithLessThanTwoPointsHasNoMatch() {
        // Comportamiento actual: progreso 0 y distancia "infinita" (Double.MAX_VALUE)
        val empty = RouteGeometry.nearestOnRoute(emptyList(), -34.0, -58.0)
        assertEquals(0f, empty.progress, 0.0001f)
        assertTrue(empty.distanceToRouteKm == Double.MAX_VALUE)

        val single = RouteGeometry.nearestOnRoute(listOf(Pair(-34.0, -58.0)), -34.0, -58.0)
        assertEquals(0f, single.progress, 0.0001f)
        assertTrue(single.distanceToRouteKm == Double.MAX_VALUE)
    }
}
