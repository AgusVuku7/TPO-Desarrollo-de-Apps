package com.chargeroute.app

import com.chargeroute.app.data.mapper.RouteMapper
import com.chargeroute.app.domain.model.RouteGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGeometryTest {

    // Ruta en forma de L: un tramo largo y uno corto
    private val route = listOf(
        Pair(-34.0, -58.0),
        Pair(-35.0, -58.0),
        Pair(-35.0, -57.9)
    )

    @Test
    fun theMiddleOfTheTripIsOnTheLongSegment() {
        // Por distancia real la mitad cae sobre el primer tramo, que es casi toda la ruta
        val point = RouteGeometry.pointAtProgress(route, 0.5f)
        assertEquals(-58.0, point.second, 0.0001)
        assertTrue(point.first < -34.0 && point.first > -35.0)
    }

    @Test
    fun pointerStartsAndEndsOnTheRoute() {
        assertEquals(route.first(), RouteGeometry.pointAtProgress(route, 0f))
        assertEquals(route.last(), RouteGeometry.pointAtProgress(route, 1f))
    }

    @Test
    fun everyPointerPositionIsOnTheLine() {
        var progress = 0f
        while (progress <= 1f) {
            val point = RouteGeometry.pointAtProgress(route, progress)
            val match = RouteGeometry.nearestOnRoute(route, point.first, point.second)
            assertTrue("El puntero se salio de la ruta", match.distanceToRouteKm < 0.01)
            progress += 0.05f
        }
    }

    @Test
    fun farPositionIsDetectedAsOffRoute() {
        val match = RouteGeometry.nearestOnRoute(route, -34.5, -57.0)
        assertTrue(match.distanceToRouteKm > 50.0)
    }

    @Test
    fun encodedPolylineComesBackTheSame() {
        val encoded = RouteMapper.encodePolyline(route)
        val decoded = RouteMapper.decodePolyline(encoded)
        assertEquals(route.size, decoded.size)
        for (i in route.indices) {
            assertEquals(route[i].first, decoded[i].first, 0.00001)
            assertEquals(route[i].second, decoded[i].second, 0.00001)
        }
    }
}
