package com.chargeroute.app

import com.chargeroute.app.domain.model.NoConnectionException
import com.chargeroute.app.domain.model.RouteServiceException
import com.chargeroute.app.domain.model.UnreachableDestinationException
import com.chargeroute.app.presentation.screens.planning.RoutePlanningError
import com.chargeroute.app.presentation.screens.planning.routeErrorFor
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutePlanningErrorTest {

    @Test
    fun noConnectionException() {
        assertEquals(RoutePlanningError.NO_CONNECTION, routeErrorFor(NoConnectionException()))
    }

    @Test
    fun routeServiceException() {
        assertEquals(RoutePlanningError.ROUTE_SERVICE, routeErrorFor(RouteServiceException()))
    }

    @Test
    fun unreachableDestinationException() {
        assertEquals(RoutePlanningError.UNREACHABLE, routeErrorFor(UnreachableDestinationException()))
    }

    @Test
    fun otherException() {
        assertEquals(RoutePlanningError.UNKNOWN, routeErrorFor(IllegalStateException("x")))
    }
}
