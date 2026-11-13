package com.chargeroute.app.presentation.preview

import com.chargeroute.app.domain.model.Connector
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.KnownCities
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.domain.model.UsedCharger
import com.chargeroute.app.domain.model.Vehicle

/**
 * Datos de ejemplo para los @Preview de las pantallas (no se usan en la app real).
 */
object PreviewData {

    val vehicle = Vehicle(
        id = "hyundai_kona_2023",
        brand = "Hyundai",
        model = "Kona Electric",
        year = 2023,
        version = "Long Range",
        usableBatteryKwh = 64.0,
        wltpRangeKm = 484.0,
        supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
        maxDcKw = 77.0
    )

    val station = Station(
        id = "ocm_1",
        remoteId = "1",
        name = "ACA Dolores",
        address = "Autovía 2 km 210, Dolores, Buenos Aires",
        latitude = -36.3132,
        longitude = -57.6792,
        connectors = listOf(Connector(ConnectorType.CCS2, 60.0), Connector(ConnectorType.TYPE_2, 22.0)),
        lastUpdatedMillis = 0L
    )

    val stop = TripStop(
        id = "stop_1",
        station = station,
        orderIndex = 0,
        stopDurationMinutes = 35,
        arrivalBatteryPercent = 19,
        departureBatteryPercent = 71,
        chargedEnergyKwh = 33.3
    )

    val routePlan = RoutePlan(
        origin = KnownCities.OBELISCO,
        destination = KnownCities.DEFAULT_DESTINATION,
        distanceKm = 414.0,
        totalDurationMinutes = 345,
        driveDurationMinutes = 310,
        chargeDurationMinutes = 35,
        stops = listOf(stop),
        polylinePoints = listOf(
            KnownCities.OBELISCO.latitude to KnownCities.OBELISCO.longitude,
            station.latitude to station.longitude,
            KnownCities.DEFAULT_DESTINATION.latitude to KnownCities.DEFAULT_DESTINATION.longitude
        ),
        encodedPolyline = "",
        arrivalBatteryPercent = 22,
        consumptionKwhPer100Km = 17.1,
        calculatedAtMillis = 0L,
        departureTime = "08:00",
        arrivalTime = "13:45",
        directDistanceKm = 400.0
    )

    val trip = Trip(
        id = "trip_preview",
        origin = KnownCities.OBELISCO,
        destination = KnownCities.DEFAULT_DESTINATION,
        vehicle = vehicle,
        initialBatteryPercent = 70,
        currentBatteryPercent = 70,
        acActive = true,
        routePlan = routePlan,
        createdAtMillis = 0L
    )

    val usedCharger = UsedCharger(
        station = station,
        tripsCount = 2,
        lastTripId = trip.id,
        lastStopId = stop.id,
        lastTripLabel = "Buenos Aires → Mar del Plata",
        lastUsedMillis = 0L
    )
}
