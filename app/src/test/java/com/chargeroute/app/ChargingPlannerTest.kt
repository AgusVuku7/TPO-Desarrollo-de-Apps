package com.chargeroute.app

import com.chargeroute.app.domain.model.ChargingPlanner
import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.StationOnRoute
import com.chargeroute.app.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingPlannerTest {

    // Auto de prueba: 50 kWh y 20 kWh/100 km => gasta 0,4 % de bateria por km (250 km con el 100 %)
    private val vehicle = Vehicle(
        id = "test",
        brand = "Test",
        model = "EV",
        year = 2024,
        version = "50 kWh",
        usableBatteryKwh = 50.0,
        wltpRangeKm = 250.0,
        supportedConnectors = listOf(ConnectorType.CCS2),
        maxDcKw = 100.0
    )
    private val consumption = 20.0

    private fun stationAt(km: Double, kw: Double = 50.0) = StationOnRoute(
        station = Station(
            id = "km_${km.toInt()}",
            remoteId = "",
            name = "Estación km ${km.toInt()}",
            address = "",
            latitude = 0.0,
            longitude = 0.0,
            connectors = listOf(Connector(ConnectorType.CCS2, kw))
        ),
        km = km
    )

    @Test
    fun shortTripNeedsNoStops() {
        // 100 km gastan 40 %: sale con 80 % y llega con 40 %
        val plan = ChargingPlanner.plan(100.0, listOf(stationAt(50.0)), vehicle, consumption, 80)

        assertTrue(plan.stops.isEmpty())
        assertEquals(40, plan.arrivalBatteryPercent)
        assertTrue(plan.reachesDestination)
    }

    @Test
    fun choosesTheFarthestStationReachableWithReserve() {
        // Con 80 % y reserva de 15 % se puede llegar hasta el km 162: elige la del km 150
        val stations = listOf(stationAt(100.0), stationAt(150.0), stationAt(190.0))

        val plan = ChargingPlanner.plan(300.0, stations, vehicle, consumption, 80)

        assertEquals(listOf(150.0), plan.stops.map { it.km })
        assertEquals(20, plan.stops[0].arrivalBatteryPercent)
        assertEquals(80, plan.stops[0].departureBatteryPercent)
        assertEquals(20, plan.arrivalBatteryPercent)
    }

    @Test
    fun longTripGetsSeveralStops() {
        // 600 km con estaciones cada 100 km: necesita parar en los km 100, 300 y 500
        val stations = listOf(100.0, 200.0, 300.0, 400.0, 500.0).map { stationAt(it) }

        val plan = ChargingPlanner.plan(600.0, stations, vehicle, consumption, 80)

        assertEquals(listOf(100.0, 300.0, 500.0), plan.stops.map { it.km })
        assertEquals(40, plan.arrivalBatteryPercent)
        assertTrue(plan.reachesDestination)
    }

    @Test
    fun withoutStationsTheTripIsMarkedAsNotReachable() {
        val plan = ChargingPlanner.plan(600.0, emptyList(), vehicle, consumption, 30)

        assertTrue(plan.stops.isEmpty())
        assertFalse(plan.reachesDestination)
        assertEquals(0, plan.arrivalBatteryPercent)
    }

    @Test
    fun chargeTimeUsesTheSlowestBetweenStationAndCar() {
        // Carga del 20 % al 80 % = 30 kWh. Estacion 50 kW, auto 100 kW => 50 kW => 36 minutos
        val plan = ChargingPlanner.plan(300.0, listOf(stationAt(150.0, kw = 50.0)), vehicle, consumption, 80)

        assertEquals(30.0, plan.stops[0].chargedEnergyKwh, 0.01)
        assertEquals(36, plan.stops[0].chargeMinutes)
    }

    @Test
    fun locatesStationsOnTheRoute() {
        // Ruta recta de 100 km: una estacion a mitad de camino queda en el km 50
        val route = listOf(Pair(-34.0, -58.0), Pair(-34.9, -58.0))
        val station = stationAt(0.0).station.copy(latitude = -34.45, longitude = -58.0)

        val located = ChargingPlanner.locateOnRoute(listOf(station), route, 100.0)

        assertEquals(50.0, located[0].km, 1.0)
    }
}
